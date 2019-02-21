package org.elasql.bench.tpce;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.vanilladb.bench.Benchmarker;
import org.vanilladb.bench.StatisticMgr;
import org.vanilladb.bench.TransactionType;
import org.vanilladb.bench.remote.SutConnection;
import org.vanilladb.bench.remote.SutDriver;
import org.vanilladb.bench.rte.RemoteTerminalEmulator;
import org.vanilladb.bench.tpce.TpceTransactionType;
import org.vanilladb.bench.tpce.data.TpceDataManager;
import org.vanilladb.bench.tpce.rte.TpceRte;

public class ElasqlTpceBenchmarker extends Benchmarker {
	
	private TpceDataManager dataMgr;
	
	// XXX: Add report postfix
	public ElasqlTpceBenchmarker(SutDriver sutDriver, int nodeId) {
		super(sutDriver);
		dataMgr = new ElasqlTpceDataManager(nodeId);
	}

	public Set<TransactionType> getBenchmarkingTxTypes() {
		Set<TransactionType> txTypes = new HashSet<TransactionType>();
		for (TransactionType txType : TpceTransactionType.values()) {
			if (txType.isBenchmarkingTx())
				txTypes.add(txType);
		}
		return txTypes;
	}

	protected void executeLoadingProcedure(SutConnection conn) throws SQLException {
		conn.callStoredProc(TpceTransactionType.SCHEMA_BUILDER.ordinal());
		conn.callStoredProc(TpceTransactionType.TESTBED_LOADER.ordinal());
	}
	
	protected RemoteTerminalEmulator createRte(SutConnection conn, StatisticMgr statMgr) {
		return new TpceRte(conn, statMgr, dataMgr);
	}
	
	protected void startProfilingProcedure(SutConnection conn) throws SQLException {
		conn.callStoredProc(TpceTransactionType.START_PROFILING.ordinal());
	}
	
	protected void stopProfilingProcedure(SutConnection conn) throws SQLException {
		conn.callStoredProc(TpceTransactionType.STOP_PROFILING.ordinal());
	}
}