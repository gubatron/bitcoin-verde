package com.softwareverde.bitcoin.server.module.stratum.api.endpoint.account;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.miner.pool.AccountId;
import com.softwareverde.bitcoin.server.Configuration;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.server.database.DatabaseConnectionFactory;
import com.softwareverde.bitcoin.server.module.stratum.api.endpoint.StratumApiResult;
import com.softwareverde.bitcoin.server.module.stratum.database.AccountDatabaseManager;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.http.HttpMethod;
import com.softwareverde.http.querystring.GetParameters;
import com.softwareverde.http.querystring.PostParameters;
import com.softwareverde.http.server.servlet.request.Request;
import com.softwareverde.http.server.servlet.response.JsonResponse;
import com.softwareverde.http.server.servlet.response.Response;
import com.softwareverde.io.Logger;
import com.softwareverde.servlet.AuthenticatedServlet;

public class PayoutAddressApi extends AuthenticatedServlet {
    protected final DatabaseConnectionFactory _databaseConnectionFactory;

    public PayoutAddressApi(final Configuration.StratumProperties stratumProperties, final DatabaseConnectionFactory databaseConnectionFactory) {
        super(stratumProperties);
        _databaseConnectionFactory = databaseConnectionFactory;
    }

    @Override
    protected Response _onAuthenticatedRequest(final AccountId accountId, final Request request) {
        final GetParameters getParameters = request.getGetParameters();
        final PostParameters postParameters = request.getPostParameters();

        if (request.getMethod() == HttpMethod.GET) {
            // GET PAYOUT ADDRESS
            // Requires GET:
            // Requires POST:

            try (final DatabaseConnection databaseConnection = _databaseConnectionFactory.newConnection()) {
                final AccountDatabaseManager accountDatabaseManager = new AccountDatabaseManager(databaseConnection);
                final Address address = accountDatabaseManager.getPayoutAddress(accountId);

                final StratumApiResult apiResult = new StratumApiResult(true, null);
                apiResult.put("address", (address != null ? address.toBase58CheckEncoded() : null));
                return new JsonResponse(Response.Codes.OK, apiResult);
            }
            catch (final DatabaseException exception) {
                Logger.log(exception);
                return new JsonResponse(Response.Codes.SERVER_ERROR, new StratumApiResult(false, "An internal error occurred."));
            }
        }
        else if (request.getMethod() == HttpMethod.POST) {
            // SET PAYOUT ADDRESS
            // Requires GET:
            // Requires POST: address

            final AddressInflater addressInflater = new AddressInflater();

            final String addressString = postParameters.get("address");
            final Address address;

            if (! addressString.isEmpty()) {
                address = addressInflater.fromBase58Check(addressString);
                if (address == null) {
                    return new JsonResponse(Response.Codes.BAD_REQUEST, new StratumApiResult(false, "Invalid address."));
                }
            }
            else {
                address = null;
            }

            try (final DatabaseConnection databaseConnection = _databaseConnectionFactory.newConnection()) {
                final AccountDatabaseManager accountDatabaseManager = new AccountDatabaseManager(databaseConnection);
                accountDatabaseManager.setPayoutAddress(accountId, address);

                return new JsonResponse(Response.Codes.OK, new StratumApiResult(true, null));
            }
            catch (final DatabaseException exception) {
                Logger.log(exception);
                return new JsonResponse(Response.Codes.SERVER_ERROR, new StratumApiResult(false, "An internal error occurred."));
            }
        }
        else {
            return new JsonResponse(Response.Codes.BAD_REQUEST, new StratumApiResult(false, "Invalid method."));
        }
    }
}
