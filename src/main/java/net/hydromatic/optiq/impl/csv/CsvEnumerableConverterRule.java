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

import net.hydromatic.optiq.rules.java.EnumerableConvention;
import net.hydromatic.optiq.rules.java.JavaRules;

import org.eigenbase.rel.RelNode;
import org.eigenbase.rel.convert.ConverterRule;
import org.eigenbase.relopt.RelOptTable;
import org.eigenbase.relopt.volcano.AbstractConverter;

public class CsvEnumerableConverterRule extends ConverterRule {

  private RelOptTable table;
  private Class<?> elementType;

  public CsvEnumerableConverterRule(RelOptTable table, Class<?> elementType) {
    super(AbstractConverter.class, CsvRel.CONVENTION,
      EnumerableConvention.INSTANCE, "Convert CSV rels to Enumerable");
    this.table = table;
    this.elementType = elementType;
  }

  @Override
  public boolean isGuaranteed() {
    return true;
  }

  @Override
  public RelNode convert(RelNode rel) {

    if (!rel.getTraitSet().contains(CsvRel.CONVENTION)) {
//      System.err.println("NOT CSVREL!");
      return null;
    }

//    System.err.println("\nConverting...");
//
//    System.err.println("Cluster... " + rel.getCluster());
//    System.err.println("TraitSet... "
//      + rel.getTraitSet().replace(getOutConvention()));
//    System.err.println("Table... " + table);

    return new JavaRules.EnumerableTableAccessRel(
      rel.getCluster()
      , rel.getTraitSet().replace(getOutConvention())
      , table
      , elementType);
  }
}
