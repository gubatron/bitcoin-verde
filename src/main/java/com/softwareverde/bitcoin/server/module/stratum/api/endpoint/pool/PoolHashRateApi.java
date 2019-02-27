package com.softwareverde.bitcoin.server.module.stratum.api.endpoint.pool;

import com.softwareverde.bitcoin.server.Configuration;
import com.softwareverde.bitcoin.server.module.stratum.api.endpoint.StratumApiEndpoint;
import com.softwareverde.bitcoin.server.module.stratum.api.endpoint.StratumApiResult;
import com.softwareverde.bitcoin.server.module.stratum.api.endpoint.StratumDataHandler;
import com.softwareverde.concurrent.pool.ThreadPool;
import com.softwareverde.servlet.GetParameters;
import com.softwareverde.servlet.PostParameters;
import com.softwareverde.servlet.request.Request;
import com.softwareverde.servlet.response.JsonResponse;
import com.softwareverde.servlet.response.Response;

import static com.softwareverde.servlet.response.Response.ResponseCodes;

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

        if (request.getMethod() != Request.HttpMethod.GET) {
            return new JsonResponse(ResponseCodes.BAD_REQUEST, new StratumApiResult(false, "Invalid method."));
        }

        {   // GET POOL HASH RATE
            // Requires GET:
            // Requires POST:

            final Long hashesPerSecond = _stratumDataHandler.getHashesPerSecond();

            final StratumApiResult apiResult = new StratumApiResult();
            apiResult.setWasSuccess(true);
            apiResult.put("hashesPerSecond", hashesPerSecond);
            return new JsonResponse(ResponseCodes.OK, apiResult);
        }
    }
}