package org.rzlabs.analysis;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.rzlabs.common.AnalysisException;
import org.rzlabs.common.CaseSensibility;
import org.rzlabs.common.NameFormat;
import org.rzlabs.common.PatternMatcher;
import org.rzlabs.io.Text;
import org.rzlabs.io.Writable;
import org.rzlabs.privilege.LdwAuth;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * https://dev.mysql.com/doc/refman/8.0/en/account-names.html
 * user name must be literally matched.
 * host name can take many forms, and wildcards are permitted.
 * cmy@%
 * cmy@192.168.%
 * cmy@[domain.name]
 */
public class UserIdentity implements Writable {
    private String user;
    private String host;
    private boolean isDomain;
    private boolean isAnalyzed = false;

    public static final UserIdentity ROOT;
    public static final UserIdentity ADMIN;
    public static final UserIdentity UNKNOWN;

    static {
        ROOT = new UserIdentity(LdwAuth.ROOT_USER, "%");
        ROOT.setIsAnalyzed();
        ADMIN = new UserIdentity(LdwAuth.ADMIN_USER, "%");
        ADMIN.setIsAnalyzed();
        UNKNOWN = new UserIdentity(LdwAuth.UNKNOWN_USER, "%");
        UNKNOWN.setIsAnalyzed();
    }

    private UserIdentity() {
    }

    public UserIdentity(String user, String host) {
        this.user = Strings.emptyToNull(user);
        this.host = Strings.emptyToNull(host);
        this.isDomain = false;
    }

    public UserIdentity(String user, String host, boolean isDomain) {
        this.user = Strings.emptyToNull(user);
        this.host = Strings.emptyToNull(host);
        this.isDomain = isDomain;
    }

    public static UserIdentity createAnalyzedUserIdentWithIp(String user, String host) {
        UserIdentity userIdentity = new UserIdentity(user, host);
        userIdentity.setIsAnalyzed();
        return userIdentity;
    }

    public static UserIdentity createAnalyzedUserIdentWithDomain(String user, String domain) {
        UserIdentity userIdentity = new UserIdentity(user, domain, true);
        userIdentity.setIsAnalyzed();
        return userIdentity;
    }

    public String getQualifiedUser() {
        Preconditions.checkState(isAnalyzed);
        return user;
    }

    public String getHost() {
        return host;
    }

    public boolean isDomain() {
        return isDomain;
    }

    public void setIsAnalyzed() {
        this.isAnalyzed = true;
    }

    public void analyze(String clusterName) throws AnalysisException {
        if (isAnalyzed) {
            return;
        }
        if (Strings.isNullOrEmpty(user)) {
            throw new AnalysisException("Does not support anonymous user");
        }

        NameFormat.checkUserName(user);
        if (!user.equals(LdwAuth.ROOT_USER) && !user.equals(LdwAuth.ADMIN_USER)) {
            //user = ClusterNamespace.getFullName(clusterName, user);
        }

        // reuse createMysqlPattern to validate host pattern
        PatternMatcher.createMysqlPattern(host, CaseSensibility.HOST.getCaseSensibility());
        isAnalyzed = true;
    }

    public static UserIdentity fromString(String userIdentStr) {
        if (Strings.isNullOrEmpty(userIdentStr)) {
            return null;
        }

        String[] parts = userIdentStr.split("@");
        if (parts.length != 2) {
            return null;
        }

        String user = parts[0];
        if (!user.startsWith("'") || !user.endsWith("'")) {
            return null;
        }

        String host = parts[1];
        if (host.startsWith("['") && host.endsWith("']")) {
            UserIdentity userIdent = new UserIdentity(user.substring(1, user.length() - 1),
                    host.substring(2, host.length() - 2), true);
            userIdent.setIsAnalyzed();
            return userIdent;
        } else if (host.startsWith("'") && host.endsWith("'")) {
            UserIdentity userIdent = new UserIdentity(user.substring(1, user.length() - 1),
                    host.substring(1, host.length() - 1));
            userIdent.setIsAnalyzed();
            return userIdent;
        }

        return null;
    }

//    public TUserIdentity toThrift() {
//        Preconditions.checkState(isAnalyzed);
//        TUserIdentity tUserIdent = new TUserIdentity();
//        tUserIdent.setHost(host);
//        tUserIdent.setUsername(user);
//        tUserIdent.setIsDomain(isDomain);
//        return tUserIdent;
//    }

    public static UserIdentity read(DataInput in) throws IOException {
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.readFields(in);
        return userIdentity;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UserIdentity)) {
            return false;
        }
        UserIdentity other = (UserIdentity) obj;
        return user.equals(other.getQualifiedUser()) && host.equals(other.getHost()) && this.isDomain == other.isDomain;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + user.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + Boolean.valueOf(isDomain).hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'");
        if (!Strings.isNullOrEmpty(user)) {
            sb.append(user);
        }
        sb.append("'@");
        if (!Strings.isNullOrEmpty(host)) {
            if (isDomain) {
                sb.append("['").append(host).append("']");
            } else {
                sb.append("'").append(host).append("'");
            }
        } else {
            sb.append("%");
        }
        return sb.toString();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        Preconditions.checkState(isAnalyzed);
        Text.writeString(out, user);
        Text.writeString(out, host);
        out.writeBoolean(isDomain);
    }

    public void readFields(DataInput in) throws IOException {
        user = Text.readString(in);
        host = Text.readString(in);
        isDomain = in.readBoolean();
        isAnalyzed = true;
    }
}
