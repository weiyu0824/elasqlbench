/*******************************************************************************
 * Copyright 2016, 2018 elasql.org contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.elasql.bench.benchmarks.tpce;

import org.vanilladb.bench.StatisticMgr;
import org.vanilladb.bench.benchmarks.tpce.TpceBenchmarker;
import org.vanilladb.bench.benchmarks.tpce.TpceTransactionType;
import org.vanilladb.bench.benchmarks.tpce.data.TpceDataManager;
import org.vanilladb.bench.benchmarks.tpce.rte.TpceRte;
import org.vanilladb.bench.remote.SutConnection;
import org.vanilladb.bench.remote.SutDriver;
import org.vanilladb.bench.rte.RemoteTerminalEmulator;

public class ElasqlTpceBenchmarker extends TpceBenchmarker {
	
	private TpceDataManager dataMgr;

	public ElasqlTpceBenchmarker(SutDriver sutDriver, int nodeId) {
		super(sutDriver, Integer.toString(nodeId));
		dataMgr = new ElasqlTpceDataManager(nodeId);
	}
	
	@Override
	protected RemoteTerminalEmulator<TpceTransactionType> createRte(SutConnection conn, StatisticMgr statMgr) {
		return new TpceRte(conn, statMgr, dataMgr);
	}
}
