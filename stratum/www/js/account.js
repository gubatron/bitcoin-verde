$(document).ready(function() {
    Ui.Account.setAuthenticated(false);
    Api.Account.validateAuthentication({ }, function(response) {
        Ui.Account.setAuthenticated(response.wasSuccess);
    });
});

(function() {
    Api.Account = { };

    Api.Account.createAccount = function(parameters, callback) {
        const defaultParameters = { email: null, password: null };
        const apiParameters = $.extend({ }, defaultParameters, parameters);

        Http.post(Api.PREFIX + "account/create", apiParameters, callback);
    };

    Api.Account.validateAuthentication = function(parameters, callback) {
        const defaultParameters = { };
        const apiParameters = $.extend({ }, defaultParameters, parameters);

        Http.post(Api.PREFIX + "account/validate", apiParameters, callback);
    };

    Api.Account.authenticate = function(parameters, callback) {
        const defaultParameters = { email: null, password: null };
        const apiParameters = $.extend({ }, defaultParameters, parameters);

        Http.post(Api.PREFIX + "account/authenticate", apiParameters, callback);
    };

    Api.Account.unauthenticate = function(parameters, callback) {
        const defaultParameters = { };
        const apiParameters = $.extend({ }, defaultParameters, parameters);

        Http.post(Api.PREFIX + "account/unauthenticate", apiParameters, callback);
    };

    Api.Account.getPayoutAddress = function(parameters, callback) {
        const defaultParameters = { };
        const apiParameters = $.extend({ }, defaultParameters, parameters);

        Http.get(Api.PREFIX + "account/address", apiParameters, callback);
    };

    Api.Account.setPayoutAddress = function(parameters, callback) {
        const defaultParameters = { address: null };
        const apiParameters = $.extend({ }, defaultParameters, parameters);

        Http.post(Api.PREFIX + "account/address", apiParameters, callback);
    };

    Api.Account.updatePassword = function(parameters, callback) {
        const defaultParameters = {
            password: null,
            newPassword: null
        };
        const apiParameters = $.extend({ }, defaultParameters, parameters);

        Http.post(Api.PREFIX + "account/password", apiParameters, callback);
    };

    Api.Account.createWorker = function(parameters, callback) {
        const defaultParameters = { username: null, password: null };
        const apiParameters = $.extend({ }, defaultParameters, parameters);

        Http.post(Api.PREFIX + "account/workers/create", apiParameters, callback);
    };

    Api.Account.deleteWorker = function(parameters, callback) {
        const defaultParameters = { workerId: null };
        const apiParameters = $.extend({ }, defaultParameters, parameters);

        Http.post(Api.PREFIX + "account/workers/delete", apiParameters, callback);
    };

    Api.Account.getWorkers = function(parameters, callback) {
        const defaultParameters = { };
        const apiParameters = $.extend({ }, defaultParameters, parameters);

        Http.get(Api.PREFIX + "account/workers", apiParameters, callback);
    };
})();

(function() {
    const onEnterSubmit = function(input, button) {
        input.on("keypress", function(event) {
            const key = event.which;
            if (key != KeyCodes.ENTER) { return true; }

            $(this).blur();

            button.trigger("click");

            return false;
        });
    };

    Ui.Account = { };

    Ui.Account.showUnauthenticatedNavigation = function() {
        const templates = $("#templates");
        const navigationContainer = $("#main .navigation ul");

        const view = $("ul.unauthenticated-navigation", templates).clone();
        const navItems = view.children();

        $(".authenticate-nav-button", view).on("click", function() {
            Ui.Account.showAuthenticateView();
        });
        $(".create-account-nav-button", view).on("click", function() {
            Ui.Account.showCreateAccountView();
        });

        navigationContainer.empty();
        navigationContainer.append(navItems);
    };

    Ui.Account.showAuthenticateView = function() {
        const templates = $("#templates");
        const viewContainer = $("#main #view-container");

        const view = $(".authenticate-container", templates).clone();

        const button = $(".submit-button", view);

        button.on("click", function() {
            Api.Account.authenticate(
                {
                    email:      $("input.authenticate-email", view).val(),
                    password:   $("input.authenticate-password", view).val()
                },
                function(data) {
                    $(".results", view).text(data.wasSuccess ? "Authenticated." : data.errorMessage);

                    if (data.wasSuccess) {
                        $(".authenticate-email", view).val("");
                        $(".authenticate-password", view).val("");

                        Ui.Account.setAuthenticated(true);
                    }
                }
            );
        });

        onEnterSubmit($("input", view), button);

        viewContainer.empty();
        viewContainer.append(view);
    };

    Ui.Account.showCreateAccountView = function() {
        const templates = $("#templates");
        const viewContainer = $("#main #view-container");

        const view = $(".create-account-container", templates).clone();

        const button = $(".submit-button", view);

        button.on("click", function() {
            Api.Account.createAccount(
                {
                    email:      $("input.create-account-email", view).val(),
                    password:   $("input.create-account-password", view).val()
                },
                function(data) {
                    $(".results", view).text(data.wasSuccess ? "Authenticated." : data.errorMessage);

                    if (data.wasSuccess) {
                        $(".create-account-email", view).val("");
                        $(".create-account-password", view).val("");

                        Ui.Account.setAuthenticated(true);
                    }
                }
            );
        });

        onEnterSubmit($("input", view), button);

        viewContainer.empty();
        viewContainer.append(view);
    };

    Ui.Account.showAuthenticatedNavigation = function() {
        const templates = $("#templates");
        const navigationContainer = $("#main .navigation ul");

        const view = $("ul.authenticated-navigation", templates).clone();
        const navItems = view.children();

        $(".set-payout-address-nav-button", view).on("click", function() {
            Ui.Account.showSetAddressView();
        });

        $(".update-password-nav-button", view).on("click", function() {
            Ui.Account.showUpdatePasswordView();
        });

        $(".manage-workers-nav-button", view).on("click", function() {
            Ui.Account.showManageWorkersView();
        });

        $(".unauthenticate-nav-button", view).on("click", function() {
            Api.Account.unauthenticate({ }, function(response) {
                Ui.Account.showUnauthenticatedNavigation();
                Ui.Account.showAuthenticateView();
            });
        });

        navigationContainer.empty();
        navigationContainer.append(navItems);
    };

    Ui.Account.showUpdatePasswordView = function() {
        const templates = $("#templates");
        const viewContainer = $("#main #view-container");

        const view = $(".update-password-container", templates).clone();

        const timeoutContainer = this;

        const button = $(".submit-button", view);

        button.on("click", function() {
            const resultsView = $(".results", view);
            window.clearTimeout(timeoutContainer.timeout);

            if ($(".new-password", view).val() != $(".confirm-new-password", view).val()) {
                resultsView.text("Passwords do not match.");
                timeoutContainer.timeout = window.setTimeout(function() {
                    resultsView.text("");
                }, 3000);

                return;
            }

            Api.Account.updatePassword(
                {
                    password: $("input.password", view).val(),
                    newPassword: $("input.new-password", view).val()
                },
                function(response) {
                    let message = "Password updated.";
                    if (! response.wasSuccess) {
                        message = response.errorMessage;
                    }

                    resultsView.text(message);
                    timeoutContainer.timeout = window.setTimeout(function() {
                        resultsView.text("");
                    }, 3000);
                }
            );
        });

        onEnterSubmit($("input", view), button);

        viewContainer.empty();
        viewContainer.append(view);

        Api.Account.getPayoutAddress({ }, function(response) {
            $("input.address", view).val(response.address);
        });
    };

    Ui.Account.showSetAddressView = function() {
        const templates = $("#templates");
        const viewContainer = $("#main #view-container");

        const view = $(".set-address-container", templates).clone();

        const timeoutContainer = this;

        const button = $(".submit-button", view);

        button.on("click", function() {
            const resultsView = $(".results", view);
            window.clearTimeout(timeoutContainer.timeout);
            Api.Account.setPayoutAddress(
                {
                    address: $("input.address", view).val()
                },
                function(response) {
                    let message = "Address updated.";
                    if (! response.wasSuccess) {
                        message = response.errorMessage;
                    }

                    resultsView.text(message);
                    timeoutContainer.timeout = window.setTimeout(function() {
                        resultsView.text("");
                    }, 3000);
                }
            );
        });

        onEnterSubmit($("input", view), button);

        viewContainer.empty();
        viewContainer.append(view);

        Api.Account.getPayoutAddress({ }, function(response) {
            $("input.address", view).val(response.address);
        });
    };

    Ui.Account.updateWorkers = function(viewContainer) {
        const templates = $("#templates");

        const headerView = $(".worker-header", templates).clone();
        const template = $(".worker", templates);
        const workersTable = $(".workers", viewContainer);

        Api.Account.getWorkers({ }, function(response) {
            workersTable.empty();
            if (! response.workers) { return; }

            if (response.workers.length) {
                workersTable.append(headerView);
            }

            for (let i in response.workers) {
                const workerData = response.workers[i];
                const view = template.clone();
                $(".id", view).text(workerData.id);
                $(".username", view).text(workerData.username);
                $(".shares-count", view).text(workerData.sharesCount);
                $(".delete", view).on("click", function() {
                    Dialog.create(
                        "Delete Worker",
                        "Do you want to delete Worker \"" + workerData.username + "\"?",
                        function() {
                            Api.Account.deleteWorker(
                                { workerId: workerData.id },
                                function() { Ui.Account.updateWorkers(viewContainer); }
                            );
                        },
                        function() { }
                    );
                });
                workersTable.append(view);
            }
        });
    };

    Ui.Account.showManageWorkersView = function() {
        const templates = $("#templates");
        const viewContainer = $("#main #view-container");

        const view = $(".manage-workers-container", templates).clone();

        const timeoutContainer = this;

        const button = $(".submit-button", view);

        button.on("click", function() {
            const resultsView = $(".results", view);
            window.clearTimeout(timeoutContainer.timeout);

            Api.Account.createWorker(
                {
                    username: $("input.username", view).val(),
                    password: $("input.password", view).val()
                },
                function(response) {
                    let message = "Worker created.";
                    if (! response.wasSuccess) {
                        message = response.errorMessage;
                    }

                    resultsView.text(message);
                    timeoutContainer.timeout = window.setTimeout(function() {
                        resultsView.text("");
                    }, 3000);

                    Ui.Account.updateWorkers(viewContainer);
                }
            );
        });

        onEnterSubmit($("input", view), button);

        viewContainer.empty();
        viewContainer.append(view);

        Ui.Account.updateWorkers(view);

        Api.Account.getPayoutAddress({ }, function(response) {
            $("input.address", view).val(response.address);
        });
    };

    Ui.Account.setAuthenticated = function(isAuthenticated) {
        const viewContainer = $("#main #view-container");

        if (isAuthenticated) {
            Ui.Account.showAuthenticatedNavigation();
            Ui.Account.showSetAddressView();
        }
        else {
            Ui.Account.showUnauthenticatedNavigation();
            Ui.Account.showAuthenticateView();
        }
    };
})();
