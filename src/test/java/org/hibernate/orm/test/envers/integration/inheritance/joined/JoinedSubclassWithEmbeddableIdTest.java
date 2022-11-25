/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined;

import org.hibernate.envers.Audited;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Jan Schatteman
 */
@ServiceRegistry(settings = {
		@Setting(name = EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD, value = "false"),
		@Setting(name = EnversSettings.AUDIT_TABLE_SUFFIX, value = "_L"),
		@Setting(name = EnversSettings.REVISION_ON_COLLECTION_CHANGE, value = "false")
})
@DomainModel(
		annotatedClasses = {
				JoinedSubclassWithEmbeddableIdTest.Parent.class,
				JoinedSubclassWithEmbeddableIdTest.Child.class,
				JoinedSubclassWithEmbeddableIdTest.ParentEmbeddedId.class
		}
)
@SessionFactory
@TestForIssue( jiraKey = "HHH-15686")
public class JoinedSubclassWithEmbeddableIdTest {

	@Test
	public void testColumnOrderingOnChildRevisionTable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ParentEmbeddedId chId = new ParentEmbeddedId();
					chId.setId( 2 );
					chId.setOtherId( 21 );
					chId.setCompany( "Acme" );

					Child c = new Child();
					c.setId( chId );
					c.setFullName( "John Doe" );
					c.setDescription( "I'm a childish entity" );

					try {
						session.persist( c );
					} catch (Exception e) {
						fail(e);
					}
				}
		);
	}

	@Entity(name = "Parent")
	@Table(name = "T_Parent")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Audited
	public static class Parent {

		@EmbeddedId
		private ParentEmbeddedId id;
		private String description;
		@Version
		private int version;

		public void setId(ParentEmbeddedId id) {
			this.id = id;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	@Embeddable
	public static class ParentEmbeddedId {
		private Integer id;
		private String company;
		private Integer otherId;

		public void setId(Integer id) {
			this.id = id;
		}

		public void setCompany(String company) {
			this.company = company;
		}

		public void setOtherId(Integer otherId) {
			this.otherId = otherId;
		}
	}

	@Entity(name = "Child")
	@Table(name = "T_Child")
	@Audited
	public static class Child extends Parent {
		private String fullName;

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}
	}

}
