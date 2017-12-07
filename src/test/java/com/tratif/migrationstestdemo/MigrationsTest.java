package com.tratif.migrationstestdemo;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.report.DiffToReport;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Catalog;
import liquibase.structure.core.DataType;

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

        filter(diff);
        
        System.out.println("====[ Diff Result ]====");
        new DiffToReport(diff, System.out).print();

        assertTrue(MISMATCH_MESSAGE, diff.areEqual());
    }

	private void filter(DiffResult diff) {
		filterUnexpectedObjects(diff);
		filterChangedObjects(diff);
	}

	/**
	 * not needed for this project, just demonstrating how filtering works
	 */
	private void filterChangedObjects(DiffResult diff) {
		Map<DatabaseObject, ObjectDifferences> changedObjects = diff.getChangedObjects();
		Map<DatabaseObject, ObjectDifferences> changedObjectsFiltered = changedObjects.entrySet().stream()
				.filter(entry -> {
					ObjectDifferences differences = entry.getValue();
			
					Difference typeDifference = differences.getDifference("type");
					if (typeDifference != null) {
						if (((DataType) typeDifference.getComparedValue()).getTypeName().equals("BIGINT") &&
								((DataType) typeDifference.getReferenceValue()).getTypeName().equals("BIGINT")) {
							// ignoring BIGINT size
							differences.removeDifference("type");
						}
					}
			
					return differences.hasDifferences();
				})
				.filter(entry -> entry.getKey().getClass() != Catalog.class) // ignoring catalog name differences
				.collect(toMap(Entry::getKey, Entry::getValue));
		changedObjects.clear();
		changedObjects.putAll(changedObjectsFiltered);
	}

	@SuppressWarnings("unchecked")
	private void filterUnexpectedObjects(DiffResult diff) {
		Set<DatabaseObject> unexpectedObjects = (Set<DatabaseObject>) diff.getUnexpectedObjects();
		Set<DatabaseObject> unexpectedObjectsFiltered = unexpectedObjects.stream()
				.filter(obj -> {
					// ignore liquibase changelog tables:
					if (obj.toString().contains("DATABASECHANGELOG")) {
						return false;
					}
					return true;
				})
				.collect(toSet());
		unexpectedObjects.clear();
		unexpectedObjects.addAll(unexpectedObjectsFiltered);
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
