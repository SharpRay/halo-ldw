package org.rzlabs.mysql;

/**
 * MySQL column type.
 * TYPE codes are defined in the file 'mysql/include/mysql_com.h' enum enum_field_types
 */
public enum MysqlColType {
    MYSQL_TYPE_DECIMAL(0, "DECIMAL"),
    MYSQL_TYPE_TINY(1, "TINY INT"),
    MYSQL_TYPE_SHORT(2, "SMALL INT"),
    MYSQL_TYPE_LONG(3, "INT"),
    MYSQL_TYPE_FLOAT(4, "FLOAT"),
    MYSQL_TYPE_DOUBLE(5, "DOUBLE"),
    MYSQL_TYPE_NULL(6, "NULL"),
    MYSQL_TYPE_TIMESTAMP(7, "TIMESTAMP"),
    MYSQL_TYPE_LONGLONG(8, "LONGLONG"),
    MYSQL_TYPE_INT24(9, "INT24"),
    MYSQL_TYPE_DATE(10, "DATE"),
    MYSQL_TYPE_TIME(11, "TIME"),
    MYSQL_TYPE_DATETIME(12, "DATETIME"),
    MYSQL_TYPE_YEAR(13, "YEAR"),
    MYSQL_TYPE_NEWDATE(14, "NEWDATE"),
    MYSQL_TYPE_VARCHAR(15, "VARCHAR"),
    MYSQL_TYPE_BIT(16, "BIT"),
    MYSQL_TYPE_TIMESTAMP2(17, "TIMESTAMP2"),
    MYSQL_TYPE_DATETIME2(18, "DATETIME2"),
    MYSQL_TYPE_TIME2(19, "TIME2"),
    MYSQL_TYPE_NEWDECIMAL(246, "NEW DECIMAL"),
    MYSQL_TYPE_ENUM(247, "ENUM"),
    MYSQL_TYPE_SET(248, "SET"),
    MYSQL_TYPE_TINY_BLOB(249, "TINY BLOB"),
    MYSQL_TYPE_MEDIUM_BLOB(250, "MEDIUM BLOB"),
    MYSQL_TYPE_LONG_BLOB(251, "LONG BLOB"),
    MYSQL_TYPE_BLOB(252, "BLOB"),
    MYSQL_TYPE_VARSTRING(253, "VAR STRING"),
    MYSQL_TYPE_STRING(254, "STRING"),
    MYSQL_TYPE_GEOMETRY(255, "GEOMETRY");

    private MysqlColType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // used in network
    private int code;
    private String desc;

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return desc;
    }
}
