/*
 * Copyright 2013-2014 ForgeRock AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 */

package org.forgerock.openam.monitoring.cts;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.monitoring.CTSReaperMonitoringStore;
import org.forgerock.openam.cts.monitoring.impl.persistence.CtsPersistenceOperationsMonitor;
import org.forgerock.openam.guice.InjectorHolder;

/**
 *
 * This class acts as the implementation of the root node of the monitoring tree structure
 * that is exposed by the monitoring service. Elements of the tree under this node are of
 * one of two types:
 *
 *  - Index tables, filled by an Enum which are simple key:value maps where the keys are
 *      increasing integers. The two index tables are:
 *
 *          - OperationTable - Contains mappings of the CTS CRUDL Operations
 *          - TokenTable - Contains mappings of the CTS token types
 *
 *  - Lookup tables, which take parameters from one or more of the index tables to provide
 *      the arguments to the functions on which
 *
 *  Upon instantiation this class fills in the index tables from the enums provided in its
 *  constructor. As such new endpoint index-combinations can be generated by adding
 *  a new entry in the Enum.
 *
 *  Finally, it generates the entries in the lookup tables so they map through to to the
 *  correct implementation.
 *
 *  To expose a new monitorable value for the CTS:
 *
 *  - Create the data structure (if necessary) to store the data (from the CTS point of view) and
 *      from which to retrieve the data (from the monitoring system's point of view)
 *
 *  - Alter the .mib endpoint structure to accommodate any new index tables (if necessary) or
 *      functional tables
 *
 *  - If the queryable function requires more then the existing index tables to reference
 *      uniquely, generate the index tables and set them up using this class
 *
 *  - Generate a new function for the exposed variable on the EntryImplementation of the
 *      table, which calls your new data structure and/or query mechanism.
 *
 *  - If we end up with more than the existing two Enums as index types, then
 *      we may want to move this signature away from forcing Enum types and using EnumSets
 *      and instead use more generic (but less well ordinated) types.
 */
public class CtsMonitoringImpl<E extends Enum<E>, F extends Enum<F>> extends CtsMonitoring {

    private static Debug debug = null; //injected and then passed on

    //our two indexes
    private EnumSet<E> crudItems;
    private EnumSet<F> tokenItems;

    private final CTSReaperMonitoringStore reaperMonitoringStore;

    /**
     * Constructor with the MBeanServer not being set to null. Generates and assigned the
     * tables to the implementations, complete with appropriate setup knowledge.
     *
     * @param myMib the mib file we are loading the snmp server from
     * @param crudItemClass enum representing crud operations
     * @param tokenItemClass enum representing tokens
     */
    public CtsMonitoringImpl(SnmpMib myMib, Class<E> crudItemClass, Class<F> tokenItemClass) {
        super(myMib);

        setCrudItems(crudItemClass);
        setTokenItems(tokenItemClass);
        init(myMib);
        this.reaperMonitoringStore = InjectorHolder.getInstance(CTSReaperMonitoringStore.class);
    }

    /**
     * Sets the Enum to use as a list of CRUDL operations
     *
     * @param crudType Class of the Enum to use for CRUD operations
     */
    private void setCrudItems(Class<E> crudType) {
        this.crudItems = EnumSet.allOf(crudType);
    }

    /**
     * Sets the Enum to use as a list of tokens
     *
     * @param tokenType Class of the Enum to use for tokens
     */
    private void setTokenItems(Class<F> tokenType) {
        this.tokenItems = EnumSet.allOf(tokenType);
    }

    /**
     * Performs the majority of the work setting up the tables
     * such that they can be queried as pass-throughs to the
     * data structures they act as queryable endpoints for.
     *
     * @param myMib The mib file which caused generation of this class
     */
    private void init(SnmpMib myMib) {
        if (debug == null) {
            final Key<Debug> key = Key.get(Debug.class, Names.named(CoreTokenConstants.CTS_MONITOR_DEBUG));
            debug = InjectorHolder.getInstance(key);
        }

        //init our index tables first
        final List<OperationEntry> operationEntries = new ArrayList<OperationEntry>();
        final List<TokenEntry> tokenEntries = new ArrayList<TokenEntry>();

        //init our CRUD operations
        for (Enum e : crudItems) {
            final OperationEntry entry = new OperationEntry(myMib);
            entry.OperationType = e.name();
            entry.OperationTableIndex = (long) e.ordinal() + 1; // +1 as OID aren't 0-based

            operationEntries.add(entry);
        }

        //init our tokens
        for (Enum e : tokenItems) {
            final TokenEntry entry = new TokenEntry(myMib);
            entry.TokenType = e.name();
            entry.TokenTableIndex = (long) e.ordinal() + 1;

            tokenEntries.add(entry);
        }

        try {
            //add each of our operations to the operation table
            for (OperationEntry ce : operationEntries) {
                OperationTable.addEntry(ce);
            }

            //add each of our tokens to the token table
            for (TokenEntry te : tokenEntries) {
                TokenTable.addEntry(te);
            }

            //init the underlying tables
            createCRUDOperationsPerTokenTypeTable(myMib, CtsCRUDOperationsPerTokenTypeTable,
                    operationEntries, tokenEntries);
            createCRUDOperationsTable(myMib, CtsCRUDOperationsTable, operationEntries);
            createTokenOperationsTable(myMib, CtsTokenOperationsTable, tokenEntries);

        } catch (SnmpStatusException e) {
            if(debug.messageEnabled()) {
                debug.error("Unable to set up CTS Monitoring tables. CTS monitoring not available.", e);
            }
        }

    }

    /**
     * Generates the endpoints for the Token Operations table. The endpoints for this table
     * take one index - the token type.
     *
     * @param myMib Mibfile from which the definition of these tables comes
     * @param table The table in to which we will write the endpoints
     * @param tokenEntries The entries from which the table will be populated
     * @throws SnmpStatusException If anything goes wrong while writing to the table
     */
    private void createTokenOperationsTable(SnmpMib myMib, TableCtsTokenOperationsTable table,
                                            List<TokenEntry> tokenEntries) throws SnmpStatusException {

        CtsPersistenceOperationsMonitor ctsPersistenceOperationsMonitor = InjectorHolder.getInstance(CtsPersistenceOperationsMonitor.class);

        for (TokenEntry te : tokenEntries) {
            final CtsTokenOperationsEntry entry = new CtsTokenOperationsEntryImpl(myMib, debug, ctsPersistenceOperationsMonitor);
            entry.TokenTableIndex = te.TokenTableIndex;

            table.addEntry(entry);
        }

    }

    /**
     * Generates the endpoints for the CRUD Operations table. The endpoints for this table
     * take one index - the operation type.
     *
     * @param myMib Mibfile from which the definition of these tables comes
     * @param table The table in to which we will write the endpoints
     * @param operationEntries The entries from which the table will be populated
     * @throws SnmpStatusException If anything goes wrong while writing to the table
     */
    private void createCRUDOperationsTable(SnmpMib myMib, TableCtsCRUDOperationsTable table,
                                           List<OperationEntry> operationEntries) throws SnmpStatusException {

        for (OperationEntry oe : operationEntries) {
              final CtsCRUDOperationsEntry entry = new CtsCRUDOperationsEntryImpl(myMib, debug);
              entry.OperationTableIndex = oe.OperationTableIndex;

              table.addEntry(entry);
        }

    }

    /**
     * Generates the endpoints for the CRUD Operations Per Token Type table. The endpoints
     * for this table take two indexes - the operation type and the token type.
     *
     * @param myMib Mibfile from which the definition of these tables comes
     * @param table The table in to which we will write the endpoints
     * @param operationEntries The operation entries from which the table will be populated
     * @param tokenEntries The token entries from which the table will be populated
     * @throws SnmpStatusException If anything goes wrong while writing to the table
     */
    private void createCRUDOperationsPerTokenTypeTable(SnmpMib myMib, TableCtsCRUDOperationsPerTokenTypeTable table,
                                                       List<OperationEntry> operationEntries,
                                                       List<TokenEntry> tokenEntries) throws SnmpStatusException {

        for (OperationEntry oe : operationEntries) {
            for (TokenEntry te : tokenEntries) {
                final CtsCRUDOperationsPerTokenTypeEntry entry = new CtsCRUDOperationsPerTokenTypeEntryImpl(myMib, debug);
                entry.OperationTableIndex = oe.OperationTableIndex;
                entry.TokenTableIndex = te.TokenTableIndex;

                table.addEntry(entry);
            }
        }

    }

    /**
     * Gets the CTS Reapers current rate of deletion from the CTS monitoring store.
     *
     * @return The Rate at which the CTS Reaper is deleting sessions.
     */
    @Override
    public Long getRateOfDeletedSessions() {
        return (long) reaperMonitoringStore.getRateOfDeletedSessions();
    }

}
