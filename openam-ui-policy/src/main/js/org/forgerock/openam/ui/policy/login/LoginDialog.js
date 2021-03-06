/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global window, define, $*/

define("org/forgerock/openam/ui/policy/login/LoginDialog", [
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants"
], function(conf, uiUtils, constants) {

    var newDialog,
        closeDialog = function() {
            newDialog.remove();
        },
        LoginDialog = {
            template: "templates/policy/LoginDialog.html",
            data : {
                reauthUrl: constants.host + "/"+ constants.context + "?goto=" + encodeURIComponent(window.location.href)
            },
            close: closeDialog,
            render: function () {
                var _this = this;

                newDialog = $("<div>");

                $("#dialogs").append(newDialog);
                newDialog.dialog({
                    title: 'Session Expired',
                    autoOpen: true,
                    open: function () {
                        uiUtils.renderTemplate(_this.template, $(this), _this.data, function () {
                            delete conf.globalData.authorizationFailurePending;
                        });
                    },
                    close: closeDialog
                });
            }
        };

    return LoginDialog;

});
