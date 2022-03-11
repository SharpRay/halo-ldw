package org.rzlabs.qe;


import org.rzlabs.analysis.CalciteStmt;
import org.rzlabs.analysis.ParseNode;
import org.rzlabs.analysis.SqlParser;
import org.rzlabs.analysis.SqlScanner;
import org.rzlabs.common.AnalysisException;
import org.rzlabs.common.DdlException;
import org.rzlabs.common.ErrorCode;
import org.rzlabs.common.ErrorReport;
import org.rzlabs.common.UserException;
import org.rzlabs.mysql.MysqlChannel;
import org.rzlabs.mysql.MysqlCommand;
import org.rzlabs.mysql.MysqlPacket;
import org.rzlabs.mysql.MysqlSerializer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;

/**
 * Process one mysql connection, receive one packet, process, send one packet.
 */
public class ConnectProcessor {
    private static final Logger LOG = LogManager.getLogger(ConnectProcessor.class);

    private final ConnectContext ctx;
    private ByteBuffer packetBuf;

    private StmtExecutor executor = null;

    public ConnectProcessor(ConnectContext context) {
        this.ctx = context;
    }

    // COM_INIT_DB: change current database of this session.
    private void handleInitDb() {
        String dbName = new String(packetBuf.array(), 1, packetBuf.limit() - 1);
        try {
            ctx.setDatabase(dbName);
        } catch (Exception e) {
            ctx.getState().setError(e.getMessage());
            return;
        }

        ctx.getState().setOk();
    }

    // COM_QUIT: set killed flag and then return OK packet.
    private void handleQuit() {
        ctx.setKilled();
        ctx.getState().setOk();
    }

    // process COM_PING statement, do nothing, just return one OK packet.
    private void handlePing() {
        ctx.getState().setOk();
    }

 //   private void auditAfterExec(String origStmt, StatementBase parsedStmt, Data.PQueryStatistics statistics) {
 //       // slow query
 //       long endTime = System.currentTimeMillis();
 //       long elapseMs = endTime - ctx.getStartTime();

 //       ctx.getAuditEventBuilder().setEventType(EventType.AFTER_QUERY)
 //               .setState(ctx.getState().toString()).setQueryTime(elapseMs)
 //               .setScanBytes(statistics == null ? 0 : statistics.getScanBytes())
 //               .setScanRows(statistics == null ? 0 : statistics.getScanRows())
 //               .setCpuTimeMs(statistics == null ? 0 : statistics.getCpuMs())
 //               .setReturnRows(ctx.getReturnRows())
 //               .setStmtId(ctx.getStmtId())
 //               .setQueryId(ctx.queryId() == null ? "NaN" : DebugUtil.printId(ctx.queryId()));

 //       if (ctx.getState().isQuery()) {
 //           MetricRepo.COUNTER_QUERY_ALL.increase(1L);
 //           if (ctx.getState().getStateType() == QueryState.MysqlStateType.ERR
 //                   && ctx.getState().getErrType() != QueryState.ErrType.ANALYSIS_ERR) {
 //               // err query
 //               MetricRepo.COUNTER_QUERY_ERR.increase(1L);
 //           } else {
 //               // ok query
 //               MetricRepo.HISTO_QUERY_LATENCY.update(elapseMs);
 //           }
 //           ctx.getAuditEventBuilder().setIsQuery(true);
 //           ctx.getQueryDetail().setEventTime(endTime);
 //           ctx.getQueryDetail().setEndTime(endTime);
 //           ctx.getQueryDetail().setLatency(elapseMs);
 //           ctx.getQueryDetail().setState(QueryDetail.QueryMemState.FINISHED);
 //           QueryDetailQueue.addOrUpdateQueryDetail(ctx.getQueryDetail());
 //       } else {
 //           ctx.getAuditEventBuilder().setIsQuery(false);
 //       }

 //       ctx.getAuditEventBuilder().setFeIp(FrontendOptions.getLocalHostAddress());

 //       // We put origin query stmt at the end of audit log, for parsing the log more convenient.
 //       if (!ctx.getState().isQuery() && (parsedStmt != null && parsedStmt.needAuditEncryption())) {
 //           ctx.getAuditEventBuilder().setStmt(parsedStmt.toSql());
 //       } else {
 //           if (parsedStmt instanceof InsertStmt && ((InsertStmt)parsedStmt).isValuesOrConstantSelect()) {
 //               // INSERT INTO VALUES may be very long, so we only log at most 1K bytes.
 //               int length = Math.min(1024, origStmt.length());
 //               ctx.getAuditEventBuilder().setStmt(origStmt.substring(0, length));
 //           } else {
 //               ctx.getAuditEventBuilder().setStmt(origStmt);
 //           }
 //       }

 //       Catalog.getCurrentAuditEventProcessor().handleAuditEvent(ctx.getAuditEventBuilder().build());
 //   }

    // process COM_QUERY statement,
    // 只有在与请求客户端交互出现问题时候才抛出异常
    private void handleQuery() {
 //       MetricRepo.COUNTER_REQUEST_ALL.increase(1L);
        // convert statement to Java string
        String originStmt = null;
        try {
            byte[] bytes = packetBuf.array();
            int ending = packetBuf.limit() - 1;
            while (ending >= 1 && bytes[ending] == '\0') {
                ending--;
            }
            originStmt = new String(bytes, 1, ending, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // impossible
            LOG.error("UTF8 is not supported in this environment.");
            ctx.getState().setError("Unsupported character set(UTF-8)");
            return;
        }
        String sqlHash = DigestUtils.md5Hex(originStmt);
        ctx.setSqlHash(sqlHash);
//        try {
//            Catalog.getCurrentCatalog().getSqlBlockRuleMgr().matchSql(originStmt, sqlHash, ctx.getQualifiedUser());
//        } catch (AnalysisException e) {
//            LOG.warn(e.getMessage());
//            ctx.getState().setError(e.getMessage());
//            return;
//        }
//        ctx.getAuditEventBuilder().reset();
//        ctx.getAuditEventBuilder()
//                .setTimestamp(System.currentTimeMillis())
//                .setClientIp(ctx.getMysqlChannel().getRemoteHostPortString())
//                .setUser(ctx.getQualifiedUser())
//                .setDb(ctx.getDatabase())
//                .setSqlHash(ctx.getSqlHash());

        // execute this query.
//        StatementBase parsedStmt = null;
//        List<Pair<StatementBase, Data.PQueryStatistics>> auditInfoList = Lists.newArrayList();
//        boolean alreadyAddedToAuditInfoList = false;
        try {
            ParseNode parsedStmt = analyze(originStmt);
            ctx.getState().reset();
            executor = new StmtExecutor(ctx, parsedStmt);
            ctx.setExecutor(executor);
            executor.execute();
//            finalizeCommand();
        } catch (IOException e) {
            // Client failed.
            LOG.warn("Process one query failed because IOException: ", e);
            ctx.getState().setError("Doris process failed");
        } catch (UserException e) {
            LOG.warn("Process one query failed because.", e);
            ctx.getState().setError(e.getMessage());
            // set is as ANALYSIS_ERR so that it won't be treated as a query failure.
            ctx.getState().setErrType(QueryState.ErrType.ANALYSIS_ERR);
        } catch (Throwable e) {
            // Catch all throwable.
            // If reach here, maybe palo bug.
            LOG.warn("Process one query failed because unknown reason: ", e);
            ctx.getState().setError(e.getClass().getSimpleName() + ", msg: " + e.getMessage());
//            if (parsedStmt instanceof KillStmt) {
//                // ignore kill stmt execute err(not monitor it)
//                ctx.getState().setErrType(QueryState.ErrType.ANALYSIS_ERR);
//            }
        }

        // that means execute some statement failed
//        if (!alreadyAddedToAuditInfoList && executor != null) {
//            auditInfoList.add(new Pair<>(executor.getParsedStmt(), executor.getQueryStatisticsForAuditLog()));
//        }
//
//        // audit after exec
//        if (!auditInfoList.isEmpty()) {
//            for (Pair<StatementBase, Data.PQueryStatistics> audit : auditInfoList) {
//                auditAfterExec(originStmt.replace("\n", " "), audit.first, audit.second);
//            }
//        } else {
//            // auditInfoList can be empty if we encounter analysis error.
//            auditAfterExec(originStmt.replace("\n", " "), null, null);
//        }
    }

    // analyze the origin stmt and return multi-statements
    //private List<StatementBase> analyze(String originStmt) throws AnalysisException, DdlException {
    private ParseNode analyze(String originStmt) throws AnalysisException, DdlException {
        LOG.debug("the originStmts are: {}", originStmt);
        // Parse statement with parser generated by CUP&FLEX
        SqlScanner input = new SqlScanner(new StringReader(originStmt), 0L);
        SqlParser parser = new SqlParser(input);
        try {
            return (ParseNode) parser.parse().value;
//        } catch (Error e) {
//            throw new AnalysisException("Please check your sql, we meet an error when parsing.", e);
//        } catch (AnalysisException | DdlException e) {
//            String errorMessage = parser.getErrorMsg(originStmt);
//            LOG.debug("origin stmt: {}; Analyze error message: {}", originStmt, parser.getErrorMsg(originStmt), e);
//            if (errorMessage == null) {
//                throw e;
//            } else {
//                throw new AnalysisException(errorMessage, e);
//            }
        } catch (Exception e) {
            try {
                return CalciteStmt.parseStmt(originStmt);
            } catch (Exception e1) {
                throw new AnalysisException(e1.getMessage());
            }
        }
    }

    // Get the column definitions of a table
    private void handleFieldList() throws IOException {
//        // Already get command code.
//        String tableName = null;
//        String pattern = null;
//        try {
//            tableName = new String(MysqlProto.readNulTerminateString(packetBuf), "UTF-8");
//            pattern = new String(MysqlProto.readEofString(packetBuf), "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            // Impossible!!!
//            LOG.error("Unknown UTF-8 character set.");
//            return;
//        }
//        if (Strings.isNullOrEmpty(tableName)) {
//            ctx.getState().setError("Empty tableName");
//            return;
//        }
//        Database db = ctx.getCatalog().getDbNullable(ctx.getDatabase());
//        if (db == null) {
//            ctx.getState().setError("Unknown database(" + ctx.getDatabase() + ")");
//            return;
//        }
//        Table table = db.getTableNullable(tableName);
//        if (table == null) {
//            ctx.getState().setError("Unknown table(" + tableName + ")");
//            return;
//        }
//
//        table.readLock();
//        try {
//            MysqlSerializer serializer = ctx.getSerializer();
//            MysqlChannel channel = ctx.getMysqlChannel();
//
//            // Send fields
//            // NOTE: Field list doesn't send number of fields
//            List<Column> baseSchema = table.getBaseSchema();
//            for (Column column : baseSchema) {
//                serializer.reset();
//                serializer.writeField(db.getFullName(), table.getName(), column, true);
//                channel.sendOnePacket(serializer.toByteBuffer());
//            }
//
//        } finally {
//            table.readUnlock();
//        }
//        ctx.getState().setEof();
    }

    private void dispatch() throws IOException {
        int code = packetBuf.get();
        MysqlCommand command = MysqlCommand.fromCode(code);
        if (command == null) {
            ErrorReport.report(ErrorCode.ERR_UNKNOWN_COM_ERROR);
            ctx.getState().setError("Unknown command(" + command + ")");
            LOG.warn("Unknown command(" + command + ")");
            return;
        }
        ctx.setCommand(command);
        ctx.setStartTime();

        switch (command) {
            case COM_INIT_DB:
                handleInitDb();
                break;
            case COM_QUIT:
                handleQuit();
                break;
            case COM_QUERY:
                handleQuery();
                break;
            case COM_FIELD_LIST:
                handleFieldList();
                break;
            case COM_PING:
                handlePing();
                break;
            default:
                ctx.getState().setError("Unsupported command(" + command + ")");
                LOG.warn("Unsupported command(" + command + ")");
                break;
        }
    }

    private ByteBuffer getResultPacket() {
        MysqlPacket packet = ctx.getState().toResponsePacket();
        if (packet == null) {
            // 当出现此种情况可能有两种可能
            // 1. 处理函数已经发送请求
            // 2. 这个协议不需要发送任何响应包
            return null;
        }

        MysqlSerializer serializer = ctx.getSerializer();
        serializer.reset();
        packet.writeTo(serializer);
        return serializer.toByteBuffer();
    }

    // 当任何一个请求完成后，一般都会需要发送一个响应包给客户端
    // 这个函数用于发送响应包给客户端
    private void finalizeCommand() throws IOException {
        ByteBuffer packet = null;
        packet = getResultPacket();
        if (executor != null && ctx.getState().getStateType() != QueryState.MysqlStateType.ERR) {
            ShowResultSet resultSet = executor.getShowResultSet();
            if (resultSet != null) {
                executor.sendResult(resultSet);
                packet = getResultPacket();
                if (packet == null) {
                    LOG.debug("packet == null");
                    return;
                }
            }
        } else {
            packet = getResultPacket();
            if (packet == null) {
                LOG.debug("packet == null");
                return;
            }
        }

        MysqlChannel channel = ctx.getMysqlChannel();
        channel.sendAndFlush(packet);
    }

//    public TMasterOpResult proxyExecute(TMasterOpRequest request) {
//        ctx.setDatabase(request.db);
//        ctx.setQualifiedUser(request.user);
//        ctx.setCatalog(Catalog.getCurrentCatalog());
//        ctx.getState().reset();
//        if (request.isSetCluster()) {
//            ctx.setCluster(request.cluster);
//        }
//        if (request.isSetResourceInfo()) {
//            ctx.getSessionVariable().setResourceGroup(request.getResourceInfo().getGroup());
//        }
//        if (request.isSetUserIp()) {
//            ctx.setRemoteIP(request.getUserIp());
//        }
//        if (request.isSetStmtId()) {
//            ctx.setForwardedStmtId(request.getStmtId());
//        }
//        if (request.isSetCurrentUserIdent()) {
//            UserIdentity currentUserIdentity = UserIdentity.fromThrift(request.getCurrentUserIdent());
//            ctx.setCurrentUserIdentity(currentUserIdentity);
//        }
//        if (request.isFoldConstantByBe()) {
//            ctx.getSessionVariable().setEnableFoldConstantByBe(request.foldConstantByBe);
//        }
//
//        if (request.isSetSessionVariables()) {
//            ctx.getSessionVariable().setForwardedSessionVariables(request.getSessionVariables());
//        } else {
//            // For compatibility, all following variables are moved to SessionVariables.
//            // Should move in future.
//            if (request.isSetTimeZone()) {
//                ctx.getSessionVariable().setTimeZone(request.getTimeZone());
//            }
//            if (request.isSetSqlMode()) {
//                ctx.getSessionVariable().setSqlMode(request.sqlMode);
//            }
//            if (request.isSetEnableStrictMode()) {
//                ctx.getSessionVariable().setEnableInsertStrict(request.enableStrictMode);
//            }
//            if (request.isSetCurrentUserIdent()) {
//                UserIdentity currentUserIdentity = UserIdentity.fromThrift(request.getCurrentUserIdent());
//                ctx.setCurrentUserIdentity(currentUserIdentity);
//            }
//            if (request.isSetInsertVisibleTimeoutMs()) {
//                ctx.getSessionVariable().setInsertVisibleTimeoutMs(request.getInsertVisibleTimeoutMs());
//            }
//        }
//
//        if (request.isSetQueryOptions()) {
//            ctx.getSessionVariable().setForwardedSessionVariables(request.getQueryOptions());
//        } else {
//            // For compatibility, all following variables are moved to TQueryOptions.
//            // Should move in future.
//            if (request.isSetExecMemLimit()) {
//                ctx.getSessionVariable().setMaxExecMemByte(request.getExecMemLimit());
//            }
//            if (request.isSetQueryTimeout()) {
//                ctx.getSessionVariable().setQueryTimeoutS(request.getQueryTimeout());
//            }
//            if (request.isSetLoadMemLimit()) {
//                ctx.getSessionVariable().setLoadMemLimit(request.loadMemLimit);
//            }
//        }
//
//        ctx.setThreadLocalInfo();
//
//        if (ctx.getCurrentUserIdentity() == null) {
//            // if we upgrade Master FE first, the request from old FE does not set "current_user_ident".
//            // so ctx.getCurrentUserIdentity() will get null, and causing NullPointerException after using it.
//            // return error directly.
//            TMasterOpResult result = new TMasterOpResult();
//            ctx.getState().setError("Missing current user identity. You need to upgrade this Frontend to the same version as Master Frontend.");
//            result.setMaxJournalId(Catalog.getCurrentCatalog().getMaxJournalId().longValue());
//            result.setPacket(getResultPacket());
//            return result;
//        }
//
//        StmtExecutor executor = null;
//        try {
//            // 0 for compatibility.
//            int idx = request.isSetStmtIdx() ? request.getStmtIdx() : 0;
//            executor = new StmtExecutor(ctx, new OriginStatement(request.getSql(), idx), true);
//            ctx.setExecutor(executor);
//            TUniqueId queryId; // This query id will be set in ctx
//            if (request.isSetQueryId()) {
//                queryId = request.getQueryId();
//            } else {
//                UUID uuid = UUID.randomUUID();
//                queryId = new TUniqueId(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
//            }
//            executor.execute(queryId);
//        } catch (IOException e) {
//            // Client failed.
//            LOG.warn("Process one query failed because IOException: ", e);
//            ctx.getState().setError("Doris process failed: " + e.getMessage());
//        } catch (Throwable e) {
//            // Catch all throwable.
//            // If reach here, maybe Doris bug.
//            LOG.warn("Process one query failed because unknown reason: ", e);
//            ctx.getState().setError("Unexpected exception: " + e.getMessage());
//        }
//        // no matter the master execute success or fail, the master must transfer the result to follower
//        // and tell the follower the current journalID.
//        TMasterOpResult result = new TMasterOpResult();
//        if (ctx.queryId() != null &&
//                // If none master FE not set query id or query id was reset in StmtExecutor when a query exec more than once,
//                // return it to none master FE.
//                (!request.isSetQueryId() || !request.getQueryId().equals(ctx.queryId()))
//        ) {
//            result.setQueryId(ctx.queryId());
//        }
//        result.setMaxJournalId(Catalog.getCurrentCatalog().getMaxJournalId().longValue());
//        result.setPacket(getResultPacket());
//        if (executor != null && executor.getProxyResultSet() != null) {
//            result.setResultSet(executor.getProxyResultSet().tothrift());
//        }
//        return result;
//    }

    // 处理一个MySQL请求，接收，处理，返回
    public void processOnce() throws IOException {
        // set status of query to OK.
        ctx.getState().reset();
//        executor = null;

        // reset sequence id of MySQL protocol
        final MysqlChannel channel = ctx.getMysqlChannel();
        channel.setSequenceId(0);
        // read packet from channel
        try {
            packetBuf = channel.fetchOnePacket();
            if (packetBuf == null) {
                LOG.warn("Null packet received from network. remote: {}", channel.getRemoteHostPortString());
                throw new IOException("Error happened when receiving packet.");
            }
        } catch (AsynchronousCloseException e) {
            // when this happened, timeout checker close this channel
            // killed flag in ctx has been already set, just return
            return;
        }

        // dispatch
        dispatch();
        // finalize
        finalizeCommand();

        ctx.setCommand(MysqlCommand.COM_SLEEP);
    }

    public void loop() {
        while (!ctx.isKilled()) {
            try {
                processOnce();
            } catch (Exception e) {
                // TODO(zhaochun): something wrong
                LOG.warn("Exception happened in one session(" + ctx + ").", e);
                ctx.setKilled();
                break;
            }
        }
    }
}
