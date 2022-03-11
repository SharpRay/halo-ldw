package org.rzlabs.common;

import com.google.common.base.Strings;
import org.rzlabs.privilege.LdwRole;

public class NameFormat {
    private static final String LABEL_REGEX = "^[-_A-Za-z0-9]{1,128}$";
    private static final String COMMON_NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9_]{0,63}$";
    private static final String COMMON_TABLE_NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9_]*$";
    private static final String COLUMN_NAME_REGEX = "^[_a-zA-Z@][a-zA-Z0-9_]{0,63}$";

    public static final String FORBIDDEN_PARTITION_NAME = "placeholder_";

//    public static void checkClusterName(String clusterName) throws AnalysisException {
//        if (Strings.isNullOrEmpty(clusterName) || !clusterName.matches(COMMON_NAME_REGEX)) {
//            ErrorReport.reportAnalysisException(ErrorCode.ERR_WRONG_CLUSTER_NAME, clusterName);
//        }
//        if (clusterName.equalsIgnoreCase(SystemInfoService.DEFAULT_CLUSTER)) {
//            ErrorReport.reportAnalysisException(ErrorCode.ERR_WRONG_CLUSTER_NAME, clusterName);
//        }
//    }

    public static void checkDbName(String dbName) throws AnalysisException {
        if (Strings.isNullOrEmpty(dbName) || !dbName.matches(COMMON_NAME_REGEX)) {
            ErrorReport.reportAnalysisException(ErrorCode.ERR_WRONG_DB_NAME, dbName);
        }
    }

    public static void checkTableName(String tableName) throws AnalysisException {
        if (Strings.isNullOrEmpty(tableName)
                || !tableName.matches(COMMON_TABLE_NAME_REGEX)
                || tableName.length() > Config.table_name_length_limit) {
            ErrorReport.reportAnalysisException(ErrorCode.ERR_WRONG_TABLE_NAME, tableName);
        }
    }

    public static void checkPartitionName(String partitionName) throws AnalysisException {
        if (Strings.isNullOrEmpty(partitionName) || !partitionName.matches(COMMON_NAME_REGEX)) {
            ErrorReport.reportAnalysisException(ErrorCode.ERR_WRONG_PARTITION_NAME, partitionName);
        }

        if (partitionName.startsWith(FORBIDDEN_PARTITION_NAME)) {
            ErrorReport.reportAnalysisException(ErrorCode.ERR_WRONG_PARTITION_NAME, partitionName);
        }
    }

//    public static void checkColumnName(String columnName) throws AnalysisException {
//        if (Strings.isNullOrEmpty(columnName) || !columnName.matches(COLUMN_NAME_REGEX)) {
//            ErrorReport.reportAnalysisException(ErrorCode.ERR_WRONG_COLUMN_NAME, columnName);
//        }
//        if (columnName.startsWith(SchemaChangeHandler.SHADOW_NAME_PRFIX)) {
//            ErrorReport.reportAnalysisException(ErrorCode.ERR_WRONG_COLUMN_NAME, columnName);
//        }
//    }

    public static void checkLabel(String label) throws AnalysisException {
        if (Strings.isNullOrEmpty(label) || !label.matches(LABEL_REGEX)) {
            throw new AnalysisException("Label format error. regex: " + LABEL_REGEX + ", label: " + label);
        }
    }

    public static void checkUserName(String userName) throws AnalysisException {
        if (Strings.isNullOrEmpty(userName) || !userName.matches(COMMON_NAME_REGEX)) {
            throw new AnalysisException("invalid user name: " + userName);
        }
    }

    public static void checkRoleName(String role, boolean canBeAdmin, String errMsg) throws AnalysisException {
        if (Strings.isNullOrEmpty(role) || !role.matches(COMMON_NAME_REGEX)) {
            throw new AnalysisException("invalid role format: " + role);
        }

        boolean res = false;
        if (CaseSensibility.ROLE.getCaseSensibility()) {
            res = role.equals(LdwRole.OPERATOR_ROLE) || (!canBeAdmin && role.equals(LdwRole.ADMIN_ROLE));
        } else {
            res = role.equalsIgnoreCase(LdwRole.OPERATOR_ROLE)
                    || (!canBeAdmin && role.equalsIgnoreCase(LdwRole.ADMIN_ROLE));
        }

        if (res) {
            throw new AnalysisException(errMsg + ": " + role);
        }
    }

    public static void checkResourceName(String resourceName) throws AnalysisException {
        checkCommonName("resource", resourceName);
    }

    public static void checkCommonName(String type, String name) throws AnalysisException {
        if (Strings.isNullOrEmpty(name) || !name.matches(COMMON_NAME_REGEX)) {
            ErrorReport.reportAnalysisException(ErrorCode.ERR_WRONG_NAME_FORMAT, type, name);
        }
    }
}
