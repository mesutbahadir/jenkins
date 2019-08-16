package tr.gov.tcmb.egmborp.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.WebSphereUowTransactionManager;

import tr.gov.tcmb.env.ServerPhase;
import tr.gov.tcmb.hibernate.session.SessionFactoryRouter;

@Configuration
public class EGMBORPWPContainerConfig {
	@Autowired
	@Qualifier("sessionFactory")
	private static SessionFactoryRouter sessionFactoryRouter;

	@Configuration
	@Profile("!TCMB.WAS")
	public static class TomcatEnv {
		private static final ServerPhase serverPhase = ServerPhase.DEVEL;

		@Bean(name = "dataSource")
		@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
		public DataSource dataSource() throws SQLException {
			DriverManagerDataSource tcDS = new DriverManagerDataSource();
			if (serverPhase.equals(ServerPhase.DEVEL)) {
				tcDS.setDriverClassName("org.apache.derby.jdbc.ClientDriver");
				tcDS.setUrl("jdbc:derby://127.0.0.1:1541/EGMBORP;create=true");
				tcDS.setUsername("EGMBORP");
				tcDS.setPassword("EGMBORP");
			} else if (serverPhase.equals(ServerPhase.TEST)) {
				tcDS.setDriverClassName("com.ibm.db2.jcc.DB2Driver");
				tcDS.setUrl("jdbc:db2://merwebt.tcmb.gov.tr:446/ANKDDB2D");
				tcDS.setUsername("EGMWBRP");
				try {
					Properties props = new Properties();
					InputStream res = this.getClass().getClassLoader()
							.getResourceAsStream("egmwbrp.txt");
					try {
						props.load(res);
						tcDS.setPassword(props.getProperty("password"));
					} finally {
						if (res != null) {
							res.close();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (serverPhase.equals(ServerPhase.PROD)) {
				tcDS.setDriverClassName("com.ibm.db2.jcc.DB2Driver");
				tcDS.setUrl("jdbc:db2://merweb.tcmb.gov.tr:447/ANKDDB2P");
				tcDS.setUsername("KSGMBTA");
				tcDS.setPassword("");
			}
			return tcDS;
		}

		@Bean(name = "transactionManager")
		@DependsOn(value = "sessionFactory")
		@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
		public PlatformTransactionManager transactionManager(
				SessionFactory sessionFactory) {
			HibernateTransactionManager transactionManager = new HibernateTransactionManager();
			transactionManager.setNestedTransactionAllowed(true);
			transactionManager.setSessionFactory(sessionFactory);
			return transactionManager;
		}
	}

	@Configuration
	@Profile("TCMB.WAS")
	public static class WebSphereEnv {
		@Bean(name = "dataSource")
		@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
		public JndiObjectFactoryBean dataSource() throws SQLException {
			JndiObjectFactoryBean targetDataSource = new JndiObjectFactoryBean();
			targetDataSource.setJndiName("jdbc/DB2_T2_EGMBORP_CP_RC");
			targetDataSource.setResourceRef(true);
			return targetDataSource;
		}

		@Bean(name = "transactionManager")
		@DependsOn(value = "sessionFactory")
		@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
		public PlatformTransactionManager transactionManager(
				SessionFactory sessionFactory) {
			WebSphereUowTransactionManager transactionManager = new WebSphereUowTransactionManager();
			return transactionManager;
		}

		@Bean(name = "nonTransactionalDataSource")
		@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
		@Lazy(value = true)
		public JndiObjectFactoryBean nonTransactionalDataSource() {
			JndiObjectFactoryBean targetDataSource = new JndiObjectFactoryBean();
			targetDataSource.setJndiName("jdbc/DB2_T2_EGMBORP_CP_NT");
			targetDataSource.setResourceRef(true);
			return targetDataSource;
		}
	}
}