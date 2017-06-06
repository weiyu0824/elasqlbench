package org.elasql.bench.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasql.bench.server.metadata.MicroBenchPartitionMetaMgr;
import org.elasql.bench.server.metadata.TpccPartitionMetaMgr;
import org.elasql.bench.server.procedure.calvin.micro.MicrobenchStoredProcFactory;
import org.elasql.bench.server.procedure.calvin.tpcc.TpccStoredProcFactory;
import org.elasql.procedure.DdStoredProcedureFactory;
import org.elasql.server.Elasql;
import org.elasql.storage.metadata.PartitionMetaMgr;
import org.vanilladb.bench.BenchmarkerParameters;
import org.vanilladb.bench.server.SutStartUp;

public class ElasqlStartUp implements SutStartUp {
	private static Logger logger = Logger.getLogger(ElasqlStartUp.class
			.getName());
	
	private static String dbName;
	private static int nodeId;
	private static boolean isSequencer;

	public void startup(String[] args) {
		if (logger.isLoggable(Level.INFO))
			logger.info("initializing benchmarker server...");
		
		try {
			parseArguments(args);
		} catch (IllegalArgumentException e) {
			System.out.println("Error: " + e.getMessage());
			System.out.println("Usage: ./startup [DB Name] [Node Id] ([Is Sequencer])");
		}
		
		Elasql.init(dbName, nodeId, isSequencer, getStoredProcedureFactory(), getPartitionMetaMgr());

		if (logger.isLoggable(Level.INFO))
			logger.info("ElaSQL server ready");
	}
	
	private static void parseArguments(String[] args) throws IllegalArgumentException {
		if (args.length < 2) {
			throw new IllegalArgumentException("The number of arguments is less than 2");
		}
		
		// #1 DB Name
		dbName = args[0];
		
		// #2 Node Id
		try {
			nodeId = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(String.format("'%s' is not a number", args[1]));
		}
		
		// #3 Is sequencer ?
		isSequencer = false;
		if (args.length > 2) {
			try {
				int num = Integer.parseInt(args[2]);
				if (num == 1)
					isSequencer = true;
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(String.format("'%s' is not a number", args[2]));
			}
		}
	}
	
	private DdStoredProcedureFactory getStoredProcedureFactory() {
		DdStoredProcedureFactory factory = null;
		switch (BenchmarkerParameters.BENCH_TYPE) {
		case MICRO:
			if (logger.isLoggable(Level.INFO))
				logger.info("using Micro-benchmark stored procedures");
			factory = new MicrobenchStoredProcFactory();
			break;
		case TPCC:
			if (logger.isLoggable(Level.INFO))
				logger.info("using TPC-C stored procedures");
			factory = new TpccStoredProcFactory();
			break;
		case TPCE:
			throw new UnsupportedOperationException("No TPC-E for now");
		}
		return factory;
	}
	
	private PartitionMetaMgr getPartitionMetaMgr() {
		PartitionMetaMgr metaMgr = null;
		switch (BenchmarkerParameters.BENCH_TYPE) {
		case MICRO:
			metaMgr = new MicroBenchPartitionMetaMgr();
			break;
		case TPCC:
			metaMgr = new TpccPartitionMetaMgr();
			break;
		case TPCE:
			throw new UnsupportedOperationException("No TPC-E for now");
		}
		return metaMgr;
	}
}