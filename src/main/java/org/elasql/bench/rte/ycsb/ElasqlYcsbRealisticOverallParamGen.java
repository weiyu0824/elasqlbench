package org.elasql.bench.rte.ycsb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import org.elasql.bench.util.ElasqlBenchProperties;
import org.elasql.bench.ycsb.ElasqlYcsbConstants;
import org.elasql.storage.metadata.PartitionMetaMgr;
import org.vanilladb.bench.TransactionType;
import org.vanilladb.bench.rte.TxParamGenerator;
import org.vanilladb.bench.tpcc.TpccValueGenerator;
import org.vanilladb.bench.util.YcsbLatestGenerator;
import org.vanilladb.bench.ycsb.YcsbConstants;
import org.vanilladb.bench.ycsb.YcsbTransactionType;

public class ElasqlYcsbRealisticOverallParamGen implements TxParamGenerator {
	private static final double RW_TX_RATE;
	private static final double DIST_TX_RATE;
	private static final double SKEW_PARAMETER;
	private static final int NUM_PARTITIONS = PartitionMetaMgr.NUM_PARTITIONS;
	
	private static final AtomicInteger[] GLOBAL_COUNTERS;
	
	// Real parameter
	private static int DATA_LEN = 51;
	private static double DATA[][] = new double[NUM_PARTITIONS][DATA_LEN];
	
	private static final long BENCH_START_TIME;
	private static final long REPLAY_PREIOD;
	private static final long WARMUP_TIME;
	private static final double SKEW_WEIGHT;
	
	private static int nodeId;
	private YcsbLatestGenerator[] latestRandoms = new YcsbLatestGenerator[NUM_PARTITIONS];
	private YcsbLatestGenerator latestRandom;
	
	static {
		RW_TX_RATE = ElasqlBenchProperties.getLoader()
				.getPropertyAsDouble(ElasqlYcsbParamGen.class.getName() + ".RW_TX_RATE", 0.0);
		SKEW_PARAMETER = ElasqlBenchProperties.getLoader()
				.getPropertyAsDouble(ElasqlYcsbParamGen.class.getName() + ".SKEW_PARAMETER", 0.0);
		
		DIST_TX_RATE = 0.1;
		
		BENCH_START_TIME = System.currentTimeMillis();
//		WARMUP_TIME = 150 * 1000;	// cause by ycsb's long init time - 100RTE
		WARMUP_TIME = 90 * 1000;	// cause by ycsb's long init time - 50RTE
		REPLAY_PREIOD = 153 * 1000;
		SKEW_WEIGHT = 6.5;
		
		// Get data
		int target[] = new int[NUM_PARTITIONS];
		for (int i = 0; i < NUM_PARTITIONS; i++) 
	      target[i] = i+1;
		 
		
		try {
			@SuppressWarnings("resource")
			BufferedReader reader = new BufferedReader(new FileReader("/opt/shared/Google_Cluster_Data.csv"));
			try {
				String line = reader.readLine();
				String item[];
				int i = 0;
				int row = 0;
				int hit = 0;
				int hitCount = 0;
				
				while (hitCount < NUM_PARTITIONS && line != null) {
					hit = 0;
					for (i = 0; i < NUM_PARTITIONS; i++) {
						if (row == target[i]) {
							hit = 1;
							hitCount++;
							break;
						}
					}
					
					if (hit == 1) {
						item = line.split(",");
						
						for (int j = 0; j < item.length; j++) {
							DATA[i][j] = Double.parseDouble(item[j]);
						}
					}
					
					line = reader.readLine();
					hit = 0;
					row++;
				}				
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	
	static {
		if (NUM_PARTITIONS == -1)
			throw new RuntimeException("it's -1 !!!!");
		
		GLOBAL_COUNTERS = new AtomicInteger[NUM_PARTITIONS];
		for (int i = 0; i < NUM_PARTITIONS; i++)
			GLOBAL_COUNTERS[i] = new AtomicInteger(0);
	}
	
	private static int getNextInsertId(int partitionId) {
		int id = GLOBAL_COUNTERS[partitionId].getAndIncrement();
		int CLIENT_COUNT = NUM_PARTITIONS;
		
		return id * CLIENT_COUNT + nodeId + getStartId(partitionId) + getRecordCount(partitionId);
	}
	
	private static int getStartId(int partitionId) {
		return partitionId * ElasqlYcsbConstants.MAX_RECORD_PER_PART + 1;
	}
	
	private static int getRecordCount(int partitionId) {
		return ElasqlYcsbConstants.RECORD_PER_PART;
	}

	public ElasqlYcsbRealisticOverallParamGen(int nodeId) {
		ElasqlYcsbRealisticOverallParamGen.nodeId = nodeId;
		for (int i = 0; i < NUM_PARTITIONS; i++) {
			int partitionSize = getRecordCount(i);
			latestRandoms[i] = new YcsbLatestGenerator(partitionSize, SKEW_PARAMETER);
		}
	}
	
	@Override
	public TransactionType getTxnType() {
		return YcsbTransactionType.YCSB;
	}


	@Override
	public Object[] generateParameter() {
		TpccValueGenerator rvg = new TpccValueGenerator();
		ArrayList<Object> paramList = new ArrayList<Object>();
		
		// ================================
		// Decide the types of transactions
		// ================================
		
		boolean isDistributedTx = (rvg.randomChooseFromDistribution(DIST_TX_RATE, 1 - DIST_TX_RATE) == 0) ? true : false;
		boolean isReadWriteTx = (rvg.randomChooseFromDistribution(RW_TX_RATE, 1 - RW_TX_RATE) == 0) ? true : false;
		
		if (NUM_PARTITIONS < 2)
			isDistributedTx = false;

		
		/////////////////////////////
		
		// =========================================
		// Decide the counts and the main partitions
		// =========================================

		// Choose the main partition
		int mainPartition = 0;
		
		long pt = (System.currentTimeMillis() - BENCH_START_TIME) - WARMUP_TIME;
		int timePoint = (int) (pt / (REPLAY_PREIOD / DATA_LEN));

		if (pt > 0 && timePoint >= 0 && timePoint < DATA_LEN) {
			mainPartition = genDistributionOfPart(timePoint);
			System.out.println("pt " + timePoint);
		}
		else {
			mainPartition = rvg.number(0, NUM_PARTITIONS - 1);
			System.out.println("Choose " + mainPartition);
		}
		
		
		
		latestRandom = latestRandoms[mainPartition];
		
		// Decide counts
		int readCount;
		int localReadCount = 2;
		int remoteReadCount = 2;
		
//		if (isReadWriteTx) 
//			readCount = 1;
		
		if (isDistributedTx)
			readCount = localReadCount+remoteReadCount;
		else
			readCount = localReadCount;
		
		// =====================
		// Generating Parameters
		// =====================
		int[] readRemoteId = new int[remoteReadCount];
		
		if (isDistributedTx) {
			for (int i = 0; i < remoteReadCount; i++) {
				int remotePartition = randomChooseOtherPartition(mainPartition, rvg);
				readRemoteId[i] = chooseARecordInMainPartition(remotePartition);
			}
		}
		
		if (isReadWriteTx) {
			int readWriteId = chooseARecordInMainPartition(mainPartition);
			int insertId = getNextInsertId(mainPartition);
			
			// Read count
			paramList.add(readCount);
			
			// Read ids (in integer)
			paramList.add(readWriteId);
			for (int i = 1; i < localReadCount; i++) {
				paramList.add(chooseARecordInMainPartition(mainPartition));
			}
			
			
//			
			
			if (isDistributedTx) {
				for (int i = 0; i < remoteReadCount; i++) {
					paramList.add(readRemoteId[i]);
				}
			}
			
			// Write count
			paramList.add(1);
			
			// Write ids (in integer)
			paramList.add(readWriteId);
			
			// Write values
			paramList.add(rvg.randomAString(YcsbConstants.CHARS_PER_FIELD));
			
			// Insert count
			paramList.add(0);
			
			// Insert ids (in integer)
			paramList.add(insertId);
			
			// Insert values
			paramList.add(rvg.randomAString(YcsbConstants.CHARS_PER_FIELD));
			
		} else {
//			int rec1Id = chooseARecordInMainPartition(mainPartition);
//			int rec2Id = rec1Id;
//			while (rec1Id == rec2Id)
//				rec2Id = chooseARecordInMainPartition(mainPartition);
			
			// Read count
			paramList.add(readCount);
			
			// Read ids (in integer)
//			paramList.add(rec1Id);
//			paramList.add(rec2Id);
			
			for (int i = 0; i < localReadCount; i++) {
				paramList.add(chooseARecordInMainPartition(mainPartition));
			}
			
			if (isDistributedTx) {
				for (int i = 0; i < remoteReadCount; i++) {
					paramList.add(readRemoteId[i]);
				}
			}
			
			// Write count
			paramList.add(0);
			
			// Insert count
			paramList.add(0);
		}
		
		return paramList.toArray(new Object[0]);
	}
	
	private int randomChooseOtherPartition(int mainPartition, TpccValueGenerator rvg) {
		return ((mainPartition + rvg.number(1, NUM_PARTITIONS - 1)) % NUM_PARTITIONS);
	}
	
	private int chooseARecordInMainPartition(int mainPartition) {
		int partitionStartId = getStartId(mainPartition);
		
		return (int) latestRandom.nextValue() + partitionStartId - 1;
	}
	
	private int genDistributionOfPart(int point) {
		LinkedList<Integer> l = new LinkedList<Integer>();
		int len = 100;
		double bot = 0;
		
		for (int i = 0; i < NUM_PARTITIONS; i++) {
		      if (i == 0)
		          bot += DATA[i][point]*SKEW_WEIGHT;
		      else
		          bot += DATA[i][point];
		   
		}
		
		for (int i = 0; i < NUM_PARTITIONS; i++) {
		      if (i == 0) {
		    	  for (int j = 0; j < len * DATA[i][point] * SKEW_WEIGHT / bot; j++)
		    		  l.add(i);
		      }
		      else {
		    	  for (int j = 0; j < len * DATA[i][point] / bot; j++)
		    		  l.add(i);
		      }
		}
		
		Collections.shuffle(l);
		return l.getFirst();
	}
}