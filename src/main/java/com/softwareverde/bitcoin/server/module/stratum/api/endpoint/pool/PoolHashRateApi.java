package com.softwareverde.bitcoin.server.module.stratum.api.endpoint.pool;

import com.softwareverde.bitcoin.server.Configuration;
import com.softwareverde.bitcoin.server.module.stratum.api.endpoint.StratumApiEndpoint;
import com.softwareverde.bitcoin.server.module.stratum.api.endpoint.StratumApiResult;
import com.softwareverde.bitcoin.server.module.stratum.api.endpoint.StratumDataHandler;
import com.softwareverde.http.HttpMethod;
import com.softwareverde.http.querystring.GetParameters;
import com.softwareverde.http.querystring.PostParameters;
import com.softwareverde.http.server.servlet.request.Request;
import com.softwareverde.http.server.servlet.response.JsonResponse;
import com.softwareverde.http.server.servlet.response.Response;

public class PoolHashRateApi extends StratumApiEndpoint {
    protected final StratumDataHandler _stratumDataHandler;

    public PoolHashRateApi(final Configuration.StratumProperties stratumProperties, final StratumDataHandler stratumDataHandler) {
        super(stratumProperties);
        _stratumDataHandler = stratumDataHandler;
    }

    @Override
    protected Response _onRequest(final Request request) {
        final GetParameters getParameters = request.getGetParameters();
        final PostParameters postParameters = request.getPostParameters();

        if (request.getMethod() != HttpMethod.GET) {
            return new JsonResponse(Response.Codes.BAD_REQUEST, new StratumApiResult(false, "Invalid method."));
        }

        {   // GET POOL HASH RATE
            // Requires GET:
            // Requires POST:

            final Long hashesPerSecond = _stratumDataHandler.getHashesPerSecond();

            final StratumApiResult apiResult = new StratumApiResult();
            apiResult.setWasSuccess(true);
            apiResult.put("hashesPerSecond", hashesPerSecond);
            return new JsonResponse(Response.Codes.OK, apiResult);
        }
    }
}
