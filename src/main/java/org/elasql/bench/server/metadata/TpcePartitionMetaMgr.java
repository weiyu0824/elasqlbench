/*******************************************************************************
 * Copyright 2016, 2017 elasql.org contributors
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
package org.elasql.bench.server.metadata;

import org.elasql.sql.RecordKey;
import org.elasql.storage.metadata.PartitionMetaMgr;
import org.vanilladb.core.sql.Constant;

public class TpcePartitionMetaMgr extends PartitionMetaMgr {

	@Override
	public boolean isFullyReplicated(RecordKey key) {
		return false;
	}

	@Override
	public int getPartition(RecordKey key) {
		String fld = key.getKeyFldSet().iterator().next();
		Constant val = key.getKeyVal(fld);
		return Math.abs(val.hashCode() % PartitionMetaMgr.NUM_PARTITIONS);
	}

}
