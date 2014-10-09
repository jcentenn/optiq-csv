/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.optiq.impl.csv;

//import net.hydromatic.optiq.rules.java.EnumerableConvention;

import org.eigenbase.rel.TableAccessRelBase;
//import org.eigenbase.relopt.ConventionTraitDef;
import org.eigenbase.relopt.RelOptCluster;
import org.eigenbase.relopt.RelOptPlanner;
import org.eigenbase.relopt.RelOptTable;
//import org.eigenbase.relopt.volcano.AbstractConverter;

public class CsvTableAccessRel extends TableAccessRelBase
  implements CsvRel {

  private static Class<?> elementType;
  private static RelOptTable table;

  public CsvTableAccessRel(RelOptCluster cluster
    , RelOptTable table, Class<?> elementType
  ) {
    super(cluster, cluster.traitSetOf(CsvRel.CONVENTION), table);

    setElementType(elementType);
    setTable(table);
  }

  @Override
  public void register(RelOptPlanner planner) {
    super.register(planner);
    registerRules(planner);
  }

  public static void registerRules(RelOptPlanner planner) {

//    System.err.println("\nRegistering rules...");
//    System.err.println(elementType);

//    planner.addRelTraitDef(EnumerableConvention.INSTANCE.getTraitDef());
//    planner.addRelTraitDef(ConventionTraitDef.INSTANCE);

    // handles actual flow planning
    planner.addRule(new CsvEnumerableConverterRule(table, elementType));
//    planner.addRule(AbstractConverter.ExpandConversionRule.INSTANCE);
  }

  public void setElementType(Class<?> elementType) {
    CsvTableAccessRel.elementType = elementType;
  }

  public void setTable(RelOptTable table) {
    CsvTableAccessRel.table = table;
  }
}
