package cn.edu.tsinghua.iotdb.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class TsfileResultMetadata implements ResultSetMetaData {
	private List<String> columnInfoList;
	private String operationType;
	private List<String> columnTypeList;
	
	public TsfileResultMetadata(List<String> columnInfoList, String operationType, List<String> columnTypeList) {
		this.columnInfoList = columnInfoList;
		this.operationType = operationType;
		this.columnTypeList = columnTypeList;
	}

	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCatalogName(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getColumnClassName(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getColumnCount() throws SQLException {
		if (columnInfoList == null || columnInfoList.size() == 0) {
			throw new SQLException("No column exists");
		}
		return columnInfoList.size();
	}

	@Override
	public int getColumnDisplaySize(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getColumnLabel(int column) throws SQLException {
		if (columnInfoList == null || columnInfoList.size() == 0) {
			throw new SQLException("No column exists");
		}
		if(column > columnInfoList.size()){
			throw new SQLException(String.format("column %d does not exist", column));
		}
		if(column <= 0) {
			throw new SQLException(String.format("column index should start from 1", column));
		}
		return columnInfoList.get(column-1);
	}

	@Override
	public String getColumnName(int column) throws SQLException {
		return getColumnLabel(column);
	}

	@Override
	public int getColumnType(int column) throws SQLException {
		// TODO Auto-generated method stub
		if (columnInfoList == null || columnInfoList.size() == 0) {
			throw new SQLException("No column exists");
		}
		if (column > columnInfoList.size()) {
			throw new SQLException(String.format("column %d does not exist", column));
		}
		if (column <= 0) {
			throw new SQLException(String.format("column index should start from 1", column));
		}

		if (column == 1) {
			return Types.TIMESTAMP;
		}
		// BOOLEAN, INT32, INT64, FLOAT, DOUBLE, TEXT,
		String columnType = columnTypeList.get(column - 2);
		switch (columnType.toUpperCase()) {
			case "BOOLEAN":
				return Types.BOOLEAN;
			case "INT32":
				return Types.INTEGER;
			case "INT64":
				return Types.BIGINT;
			case "FLOAT":
				return Types.FLOAT;
			case "DOUBLE":
				return Types.DOUBLE;
			case "TEXT":
				return Types.VARCHAR;
			default:
				break;
			}
			return 0;
	}

	@Override
	public String getColumnTypeName(int arg0) throws SQLException {
		return operationType;
	}

	@Override
	public int getPrecision(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getScale(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getSchemaName(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTableName(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAutoIncrement(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCaseSensitive(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCurrency(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDefinitelyWritable(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int isNullable(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isReadOnly(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSearchable(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSigned(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isWritable(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

}
