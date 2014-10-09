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

import net.hydromatic.optiq.SchemaPlus;
import net.hydromatic.optiq.Statistic;
import net.hydromatic.optiq.Statistics;
import net.hydromatic.optiq.TranslatableTable;
import net.hydromatic.optiq.impl.AbstractTableQueryable;
import net.hydromatic.optiq.impl.java.AbstractQueryableTable;
import net.hydromatic.optiq.impl.java.JavaTypeFactory;
//import net.hydromatic.optiq.rules.java.EnumerableConvention;

import net.hydromatic.linq4j.AbstractEnumerable;
import net.hydromatic.linq4j.Enumerable;
import net.hydromatic.linq4j.Enumerator;
import net.hydromatic.linq4j.QueryProvider;
import net.hydromatic.linq4j.Queryable;

import org.eigenbase.rel.RelNode;
import org.eigenbase.relopt.RelOptCluster;
import org.eigenbase.relopt.RelOptTable;
import org.eigenbase.reltype.*;
import org.eigenbase.util.Pair;

import au.com.bytecode.opencsv.CSVReader;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Table based on a CSV file.
 */
public class CsvTable extends AbstractQueryableTable
    implements TranslatableTable {
  private final File file;
  private final RelProtoDataType protoRowType;
  private List<CsvFieldType> fieldTypes;

  /** Creates a CsvTable. */
  CsvTable(File file, RelProtoDataType protoRowType) {
    super(Object[].class);
    this.file = file;
    this.protoRowType = protoRowType;
  }

  public String toString() {
    return "CsvTable";
  }

  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    if (protoRowType != null) {
      return protoRowType.apply(typeFactory);
    }
    if (fieldTypes == null) {
      fieldTypes = new ArrayList<CsvFieldType>();
      return deduceRowType((JavaTypeFactory) typeFactory, file, fieldTypes);
    } else {
      return deduceRowType((JavaTypeFactory) typeFactory, file, null);
    }
  }

  public Statistic getStatistic() {
    return Statistics.UNKNOWN;
  }

  public <T> Queryable<T> asQueryable(QueryProvider queryProvider,
      SchemaPlus schema, String tableName) {
    return new AbstractTableQueryable<T>(queryProvider, schema, this,
        tableName) {
      @SuppressWarnings("unchecked")
      public Enumerator<T> enumerator() {
        //noinspection unchecked
        return (Enumerator<T>) new CsvEnumerator(file,
            fieldTypes.toArray(new CsvFieldType[fieldTypes.size()]));
      }
    };
  }

  /** Returns an enumerable over a given projection of the fields. */
  public Enumerable<Object> project(final int[] fields) {
    return new AbstractEnumerable<Object>() {
      public Enumerator<Object> enumerator() {
        return new CsvEnumerator(file,
            fieldTypes.toArray(new CsvFieldType[fieldTypes.size()]), fields);
      }
    };
  }

  public RelNode toRel(
      RelOptTable.ToRelContext context,
      RelOptTable relOptTable) {

    RelOptCluster cluster = context.getCluster();

//    System.err.println("\n\ntoRel");
//
//    System.err.println("Cluster: " + cluster);
//    System.err.println("TraitSet: "
//      + cluster.traitSetOf(EnumerableConvention.INSTANCE));
//    System.err.println("Table: " + relOptTable);

    return new CsvTableAccessRel(cluster
      , relOptTable
      , (Class<?>) getElementType()
    );
  }

  /** Deduces the names and types of a table's columns by reading the first line
   * of a CSV file. */
  static RelDataType deduceRowType(JavaTypeFactory typeFactory, File file,
      List<CsvFieldType> fieldTypes) {
    final List<RelDataType> types = new ArrayList<RelDataType>();
    final List<String> names = new ArrayList<String>();
    CSVReader reader = null;
    try {
      final FileReader fileReader;
      if (file.getName().endsWith(".gz")) {
        final GZIPInputStream inputStream =
            new GZIPInputStream(new FileInputStream(file));
        reader = new CSVReader(new InputStreamReader(inputStream));
      } else {
        fileReader = new FileReader(file);
        reader = new CSVReader(fileReader);
      }
      final String[] strings = reader.readNext();
      for (String string : strings) {
        final String name;
        final CsvFieldType fieldType;
        final int colon = string.indexOf(':');
        if (colon >= 0) {
          name = string.substring(0, colon);
          String typeString = string.substring(colon + 1);
          fieldType = CsvFieldType.of(typeString);
        } else {
          name = string;
          fieldType = null;
        }
        final RelDataType type;
        if (fieldType == null) {
          type = typeFactory.createJavaType(String.class);
        } else {
          type = fieldType.toType(typeFactory);
        }
        names.add(name);
        types.add(type);
        if (fieldTypes != null) {
          fieldTypes.add(fieldType);
        }
      }
    } catch (IOException e) {
      // ignore
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    if (names.isEmpty()) {
      names.add("line");
      types.add(typeFactory.createJavaType(String.class));
    }
    return typeFactory.createStructType(Pair.zip(names, types));
  }
}

// End CsvTable.java
