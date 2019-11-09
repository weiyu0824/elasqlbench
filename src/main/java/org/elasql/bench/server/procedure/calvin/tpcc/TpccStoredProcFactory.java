package org.elasql.bench.server.procedure.calvin.tpcc;

import org.elasql.bench.server.procedure.calvin.StartProfilingProc;
import org.elasql.bench.server.procedure.calvin.StopProfilingProc;
import org.elasql.procedure.calvin.CalvinStoredProcedure;
import org.elasql.procedure.calvin.CalvinStoredProcedureFactory;
import org.elasql.server.migration.procedure.AsyncMigrateProc;
import org.elasql.server.migration.procedure.BroadcastMigrationKeysProc;
import org.elasql.server.migration.procedure.LaunchClayProc;
import org.elasql.server.migration.procedure.MigrationAnalysisProc;
import org.elasql.server.migration.procedure.StartMigrationProc;
import org.elasql.server.migration.procedure.StopMigrationProc;
import org.vanilladb.bench.tpcc.TpccTransactionType;

public class TpccStoredProcFactory implements CalvinStoredProcedureFactory {

	@Override
	public CalvinStoredProcedure<?> getStoredProcedure(int pid, long txNum) {
		CalvinStoredProcedure<?> sp;
		switch (TpccTransactionType.fromProcedureId(pid)) {
		case SCHEMA_BUILDER:
			sp = new TpccSchemaBuilderProc(txNum);
			break;
		case TESTBED_LOADER:
			sp = new TpccTestbedLoaderProc(txNum);
			break;
		case START_PROFILING:
			sp = new StartProfilingProc(txNum);
			break;
		case STOP_PROFILING:
			sp = new StopProfilingProc(txNum);
			break;
		case NEW_ORDER:
			sp = new NewOrderProc(txNum);
			break;
		case PAYMENT:
			sp = new PaymentProc(txNum);
			break;
		case START_MIGRATION:
			sp = new StartMigrationProc(txNum);
			break;
		case STOP_MIGRATION:
			sp = new StopMigrationProc(txNum);
			break;
		case ASYNC_MIGRATE:
			sp = new AsyncMigrateProc(txNum);
			break;
		case MIGRATION_ANALYSIS:
			sp = new MigrationAnalysisProc(txNum);
			break;
		case LAUNCH_CLAY:
			sp = new LaunchClayProc(txNum);
			break;
		case BROADCAST_MIGRAKEYS:
			sp = new BroadcastMigrationKeysProc(txNum);
			break;
		default:
			throw new UnsupportedOperationException("Procedure " + TpccTransactionType.fromProcedureId(pid) + " is not supported for now");
		}
		return sp;
	}
}
