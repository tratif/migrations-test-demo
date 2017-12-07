package com.tratif.migrationstestdemo;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.report.DiffToReport;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class MigrationsTest {

    private static final String MISMATCH_MESSAGE = "There is a mismatch between Hibernate and Liquibase schemas. Check the diff result in the log output.";
    private static final String CHANGELOG_LOCATION = "db_migration_scripts/schemaChangelog.xml";

    @Autowired
    DataSource dataSource;

    DriverManagerDataSource liquibaseDs;

    
    @Test
    public void testMigrations() throws Exception {
        Connection liquibaseCon = getDbForLiquibase();
        runDbScriptsOnLiquibaseDb();

        Connection reference = dataSource.getConnection();

        DiffResult diff = getLiquibase().diff(getDb(reference), getDb(liquibaseCon), compareControl());

        System.out.println("====[ Diff Result ]====");
        new DiffToReport(diff, System.out).print();

        assertTrue(MISMATCH_MESSAGE, diff.areEqual());
    }

	private CompareControl compareControl() {
		CompareControl compareControl = new CompareControl();
		return compareControl;
	}

    private Connection getDbForLiquibase() throws SQLException {
        liquibaseDs = new DriverManagerDataSource("jdbc:hsqldb:mem:migrationsTestDb");
        liquibaseDs.setUsername("sa");
        liquibaseDs.setPassword("");
        return liquibaseDs.getConnection();
    }

    private static Database getDb(Connection con) {
        try {
            return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(con));
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        }
    }

    private void update(Liquibase liquibase) {
        try {
            liquibase.update("migrationTest");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void runDbScriptsOnLiquibaseDb() {
        update(getLiquibase());
    }

    private Liquibase getLiquibase() {
        try {
            return new Liquibase(CHANGELOG_LOCATION, new ClassLoaderResourceAccessor(), getDb(getDbForLiquibase()));
        } catch (LiquibaseException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
