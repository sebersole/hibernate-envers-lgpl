/**
 * 
 */
package org.hibernate.envers.test.integration.collection;

import static org.junit.Assert.assertEquals;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.entities.collection.EmbeddableSetEntity;
import org.hibernate.envers.test.entities.components.Component3;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class EmbeddableSet extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EmbeddableSetEntity.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9199")
	@FailureExpected(jiraKey = "HHH-9199")
	public void testRemoval() {
		EntityManager em = getEntityManager();

		final Component3 comp1 = new Component3( "comp1", null, null );
		final Component3 comp2 = new Component3( "comp2", null, null );

		EmbeddableSetEntity entity = new EmbeddableSetEntity();

		em.getTransaction().begin();

		entity.getComponentSet().add( comp1 );
		entity.getComponentSet().add( comp2 );

		em.persist( entity );

		em.getTransaction().commit();

		em.getTransaction().begin();

		entity.getComponentSet().remove( comp1 );

		em.getTransaction().commit();

		EmbeddableSetEntity rev1 = getAuditReader().find( EmbeddableSetEntity.class, entity.getId(), 1 );
		EmbeddableSetEntity rev2 = getAuditReader().find( EmbeddableSetEntity.class, entity.getId(), 2 );
		assertEquals( "Unexpected components", TestTools.makeSet( comp1, comp2 ), rev1.getComponentSet() );
		assertEquals( "Unexpected components", TestTools.makeSet( comp2 ), rev2.getComponentSet() );
	}

}
