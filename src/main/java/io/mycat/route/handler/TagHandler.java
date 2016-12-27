package io.mycat.route.handler;

import io.mycat.MycatServer;
import io.mycat.backend.datasource.PhysicalDBNode;
import io.mycat.backend.mysql.nio.handler.SingleNodeHandler;
import io.mycat.cache.LayerCachePool;
import io.mycat.config.model.SchemaConfig;
import io.mycat.config.model.SystemConfig;
import io.mycat.route.RouteResultset;
import io.mycat.route.RouteResultsetNode;
import io.mycat.route.util.RouterUtil;
import io.mycat.server.ServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLNonTransientException;
import java.util.Map;
import java.util.Set;

/**
 * Created by jackychenb on 27/12/2016.
 */
public class TagHandler implements HintHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagHandler.class);

    @Override
    public RouteResultset route(SystemConfig sysConfig, SchemaConfig schema, int sqlType, String realSQL, String charset, ServerConnection sc, LayerCachePool cachePool, String hintSQLValue, int hintSqlType, Map hintMap) throws SQLNonTransientException {
        if (!"current_data_node".equalsIgnoreCase(hintSQLValue)) {
            String msg = "The mycat:tag value of '" + hintSQLValue + "' is NOT supported";
            LOGGER.error(msg);
            throw new SQLNonTransientException(msg);
        }

//        Set<RouteResultsetNode> nodes = sc.getSession2().getTargetKeys();
//        if (nodes.size() == 1) {
//            RouteResultsetNode node = nodes.toArray(new RouteResultsetNode[1])[0];
        SingleNodeHandler handler = sc.getSession2().getSingleNodeHandler();
        if (handler != null) {
            RouteResultsetNode node = handler.getNode();
            RouteResultset rrs = new RouteResultset(realSQL, sqlType);
            return RouterUtil.routeToSingleNode(rrs, node.getName(), realSQL);
        }

        String msg = "The mycat:tag hint should be only used after the data node has been routed";
        LOGGER.error(msg);
        throw new SQLNonTransientException(msg);
    }

}
