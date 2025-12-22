package uk.gov.moj.cpp.prosecution.casefile.helper;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.fileservice.utils.test.FileServiceTestClient;
import uk.gov.justice.services.test.utils.common.host.TestHostProvider;
import uk.gov.justice.services.test.utils.core.jdbc.JdbcConnectionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class FileServiceHelper {

    private static final FileServiceTestClient client = new FileServiceTestClient();
    private static final JdbcConnectionProvider connectionProvider = new JdbcConnectionProvider();
    private static Properties properties = new Properties();
    private static String user;
    private static String password;
    private static String driverClassName;
    private static String connectionString;

    static {
        try {
            properties.load(FileServiceHelper.class.getClassLoader().getResourceAsStream("fileservice-db.properties"));
            final String connectionStringTemplate = properties.getProperty("connectionStringTemplate", "jdbc:postgresql://%s/fileservice");
            user = properties.getProperty("user", "fileservice");
            password = properties.getProperty("password", "fileservice");
            driverClassName = properties.getProperty("driverClassName", "org.postgresql.Driver");
            connectionString = String.format(connectionStringTemplate, TestHostProvider.getHost());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Optional<FileReference> read(final UUID fileId) throws SQLException, FileServiceException {
        try (final Connection connection = connectionProvider.getConnection(connectionString, user, password, driverClassName)) {
            return client.read(fileId, connection);
        }
    }

    public static UUID create(final String fileName, final String mediaType, final InputStream contentStream) throws SQLException, FileServiceException {
        try (final Connection connection = connectionProvider.getConnection(connectionString, user, password, driverClassName)) {
            return client.create(fileName, mediaType, contentStream, connection);
        }
    }
}
