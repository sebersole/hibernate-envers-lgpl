/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.ComponentDescription;
import org.hibernate.envers.internal.entities.ComponentDescription.ComponentType;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.AuditAssociationQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.internal.CriteriaTools;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 * @author Chris Cranford
 */
@Incubating
public class AuditAssociationQueryImpl<Q extends AuditQueryImplementor>
		implements AuditAssociationQuery<Q>, AuditQueryImplementor {

	private final EnversService enversService;
	private final AuditReaderImplementor auditReader;
	private final Q parent;
	private final QueryBuilder queryBuilder;
	private final JoinType joinType;
	private final String entityName;
	private final RelationDescription relationDescription;
	private final ComponentDescription componentDescription;
	private final String ownerAlias;
	private final String ownerEntityName;
	private final String alias;
	private final Map<String, String> aliasToEntityNameMap;
	private final Map<String, String> aliasToComponentPropertyNameMap;
	private final List<AuditCriterion> criterions = new ArrayList<>();
	private final Parameters parameters;
	private final List<AuditAssociationQueryImpl<?>> associationQueries = new ArrayList<>();
	private final Map<String, AuditAssociationQueryImpl<AuditAssociationQueryImpl<Q>>> associationQueryMap = new HashMap<>();

	public AuditAssociationQueryImpl(
			final EnversService enversService,
			final AuditReaderImplementor auditReader,
			final Q parent,
			final QueryBuilder queryBuilder,
			final String propertyName,
			final JoinType joinType,
			final Map<String, String> aliasToEntityNameMap,
			final Map<String, String> aliasToComponentPropertyNameMap,
			final String ownerAlias,
			final String userSuppliedAlias) {
		this.enversService = enversService;
		this.auditReader = auditReader;
		this.parent = parent;
		this.queryBuilder = queryBuilder;
		this.joinType = joinType;

		ownerEntityName = aliasToEntityNameMap.get( ownerAlias );
		this.ownerAlias = ownerAlias;
		this.alias = userSuppliedAlias == null ? queryBuilder.generateAlias() : userSuppliedAlias;

		String componentPrefix = CriteriaTools.determineComponentPropertyPrefix(
				enversService,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				ownerAlias
		);
		String prefixedPropertyName = componentPrefix.concat( propertyName );

		relationDescription = CriteriaTools.getRelatedEntity(
				enversService,
				ownerEntityName,
				prefixedPropertyName
		);

		componentDescription = CriteriaTools.getComponent( enversService, ownerEntityName, prefixedPropertyName );

		if ( relationDescription == null && componentDescription == null ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ENGLISH,
							"Property %s of entity %s is not a valid association for queries",
							propertyName,
							ownerEntityName
					)
			);
		}

		if ( relationDescription != null ) {
			this.entityName = relationDescription.getToEntityName();
		}
		else {
			aliasToComponentPropertyNameMap.put( alias, componentDescription.getPropertyName() );
			this.entityName = ownerEntityName;
		}
		aliasToEntityNameMap.put( this.alias, entityName );
		this.aliasToEntityNameMap = aliasToEntityNameMap;
		this.aliasToComponentPropertyNameMap = aliasToComponentPropertyNameMap;
		parameters = queryBuilder.addParameters( this.alias );
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public List getResultList() throws AuditException {
		return parent.getResultList();
	}

	@Override
	public Object getSingleResult() throws AuditException, NonUniqueResultException, NoResultException {
		return parent.getSingleResult();
	}

	@Override
	public AuditAssociationQueryImpl<AuditAssociationQueryImpl<Q>> traverseRelation(
			String associationName,
			JoinType joinType) {
		return traverseRelation(
				associationName,
				joinType,
				null
		);
	}

	@Override
	public AuditAssociationQueryImpl<AuditAssociationQueryImpl<Q>> traverseRelation(
			String associationName,
			JoinType joinType,
			String alias) {
		AuditAssociationQueryImpl<AuditAssociationQueryImpl<Q>> result = associationQueryMap.get( associationName );
		if ( result == null ) {
			result = new AuditAssociationQueryImpl<>(
					enversService,
					auditReader,
					this,
					queryBuilder,
					associationName,
					joinType,
					aliasToEntityNameMap,
					aliasToComponentPropertyNameMap,
					this.alias,
					alias
			);
			associationQueries.add( result );
			associationQueryMap.put( associationName, result );
		}
		return result;
	}

	@Override
	public AuditAssociationQueryImpl<Q> add(AuditCriterion criterion) {
		criterions.add( criterion );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> addProjection(AuditProjection projection) {
		String projectionEntityAlias = projection.getAlias( alias );
		String projectionEntityName = aliasToEntityNameMap.get( projectionEntityAlias );
		registerProjection( projectionEntityName, projection );
		projection.addProjectionToQuery(
				enversService,
				auditReader,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				alias,
				queryBuilder
		);
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> addOrder(AuditOrder order) {
		AuditOrder.OrderData orderData = order.getData( enversService.getConfig() );
		String orderEntityAlias = orderData.getAlias( alias );
		String orderEntityName = aliasToEntityNameMap.get( orderEntityAlias );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				auditReader,
				orderEntityName,
				orderData.getPropertyName()
		);
		String componentPrefix = CriteriaTools.determineComponentPropertyPrefix(
				enversService,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				orderEntityAlias
		);
		queryBuilder.addOrder(
				orderEntityAlias,
				componentPrefix.concat( propertyName ),
				orderData.isAscending(),
				orderData.getNullPrecedence()
		);
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setMaxResults(int maxResults) {
		parent.setMaxResults( maxResults );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setFirstResult(int firstResult) {
		parent.setFirstResult( firstResult );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setCacheable(boolean cacheable) {
		parent.setCacheable( cacheable );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setCacheRegion(String cacheRegion) {
		parent.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setComment(String comment) {
		parent.setComment( comment );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setFlushMode(FlushMode flushMode) {
		parent.setFlushMode( flushMode );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setCacheMode(CacheMode cacheMode) {
		parent.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setTimeout(int timeout) {
		parent.setTimeout( timeout );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setLockMode(LockMode lockMode) {
		parent.setLockMode( lockMode );
		return this;
	}

	public Q up() {
		return parent;
	}

	protected void addCriterionsToQuery(AuditReaderImplementor versionsReader) {
		if ( relationDescription != null ) {
			createEntityJoin( enversService.getConfig() );
		}
		else {
			createComponentJoin( enversService.getConfig() );
		}

		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery(
					enversService,
					versionsReader,
					aliasToEntityNameMap,
					aliasToComponentPropertyNameMap,
					alias,
					queryBuilder,
					parameters
			);
		}

		for ( AuditAssociationQueryImpl<?> query : associationQueries ) {
			query.addCriterionsToQuery( versionsReader );
		}
	}

	private void createEntityJoin(Configuration configuration) {
		boolean targetIsAudited = enversService.getEntitiesConfigurations().isVersioned( entityName );
		String targetEntityName = entityName;
		if ( targetIsAudited ) {
			targetEntityName = configuration.getAuditEntityName( entityName );
		}
		String originalIdPropertyName = configuration.getOriginalIdPropertyName();
		String revisionPropertyPath = configuration.getRevisionNumberPath();

		if ( relationDescription.getRelationType() == RelationType.TO_ONE ) {
			Parameters joinConditionParameters = queryBuilder.addJoin( joinType, targetEntityName, alias, false );

			// owner.reference_id = target.originalId.id
			IdMapper idMapperTarget;
			String prefix;
			if ( targetIsAudited ) {
				idMapperTarget = enversService.getEntitiesConfigurations().get( entityName ).getIdMapper();
				prefix = alias.concat( "." ).concat( originalIdPropertyName );
			}
			else {
				idMapperTarget = enversService.getEntitiesConfigurations()
						.getNotVersionEntityConfiguration( entityName )
						.getIdMapper();
				prefix = alias;
			}
			relationDescription.getIdMapper().addIdsEqualToQuery(
					joinConditionParameters,
					ownerAlias,
					idMapperTarget,
					prefix
			);
		}
		else if ( relationDescription.getRelationType() == RelationType.TO_MANY_NOT_OWNING ) {
			if ( !targetIsAudited ) {
				throw new AuditException(
						String.format(
								Locale.ENGLISH,
								"Cannot build queries for relation type %s to non audited target entities",
								relationDescription.getRelationType()
						)
				);
			}
			Parameters joinConditionParameters = queryBuilder.addJoin( joinType, targetEntityName, alias, false );

			// owner.originalId.id = target.reference_id
			IdMapper idMapperOwner = enversService.getEntitiesConfigurations().get( ownerEntityName ).getIdMapper();
			String prefix = ownerAlias.concat( "." ).concat( originalIdPropertyName );
			relationDescription.getIdMapper().addIdsEqualToQuery(
					joinConditionParameters,
					alias,
					idMapperOwner,
					prefix );
		}
		else if ( relationDescription.getRelationType() == RelationType.TO_MANY_MIDDLE
				|| relationDescription.getRelationType() == RelationType.TO_MANY_MIDDLE_NOT_OWNING ) {
			if ( !targetIsAudited && relationDescription.getRelationType() == RelationType.TO_MANY_MIDDLE_NOT_OWNING ) {
				throw new AuditException(
						String.format(
								Locale.ENGLISH,
								"Cannot build queries for relation type %s to non audited target entities",
								relationDescription.getRelationType()
						)
				);
			}

			String middleEntityAlias = queryBuilder.generateAlias();

			// join middle_entity
			Parameters joinConditionParametersMiddle = queryBuilder.addJoin(
					joinType,
					relationDescription.getAuditMiddleEntityName(),
					middleEntityAlias,
					false
			);

			// join target_entity
			Parameters joinConditionParametersTarget = queryBuilder.addJoin( joinType, targetEntityName, alias, false );

			Parameters middleParameters = queryBuilder.addParameters( middleEntityAlias );
			String middleOriginalIdPropertyPath = middleEntityAlias + "." + originalIdPropertyName;

			// join condition: owner.reference_id = middle.id_ref_ing
			String ownerPrefix = ownerAlias + "." + originalIdPropertyName;
			MiddleIdData referencingIdData = relationDescription.getReferencingIdData();
			referencingIdData.getPrefixedMapper().addIdsEqualToQuery(
					joinConditionParametersMiddle,
					middleOriginalIdPropertyPath,
					referencingIdData.getOriginalMapper(),
					ownerPrefix
			);

			// join condition: middle.id_ref_ed = target.id
			String targetPrefix = alias;
			if ( targetIsAudited ) {
				targetPrefix = alias + "." + originalIdPropertyName;
			}
			MiddleIdData referencedIdData = relationDescription.getReferencedIdData();
			referencedIdData.getPrefixedMapper().addIdsEqualToQuery(
					joinConditionParametersTarget,
					middleOriginalIdPropertyPath,
					referencedIdData.getOriginalMapper(),
					targetPrefix
			);

			// filter revisions of middle entity
			Parameters middleParametersToUse = middleParameters;
			if ( joinType == JoinType.LEFT ) {
				middleParametersToUse = middleParameters.addSubParameters( Parameters.OR );
				middleParametersToUse.addNullRestriction( revisionPropertyPath, true );
				middleParametersToUse = middleParametersToUse.addSubParameters( Parameters.AND );
			}

			enversService.getAuditStrategy().addAssociationAtRevisionRestriction(
					queryBuilder,
					middleParametersToUse,
					revisionPropertyPath,
					configuration.getRevisionEndFieldName(),
					true,
					referencingIdData,
					relationDescription.getAuditMiddleEntityName(),
					middleOriginalIdPropertyPath,
					revisionPropertyPath,
					originalIdPropertyName,
					middleEntityAlias,
					true
			);

			// filter deleted middle entities
			if ( joinType == JoinType.LEFT ) {
				middleParametersToUse = middleParameters.addSubParameters( Parameters.OR );
				middleParametersToUse.addNullRestriction( configuration.getRevisionTypePropertyName(), true );
			}
			middleParametersToUse.addWhereWithParam( configuration.getRevisionTypePropertyName(), true, "!=", RevisionType.DEL );
		}
		else {
			throw new AuditException(
					String.format(
							Locale.ENGLISH,
							"Cannot build queries for relation type %s",
							relationDescription.getRelationType()
					)
			);
		}

		if ( targetIsAudited ) {
			// filter revision of target entity
			Parameters parametersToUse = parameters;
			if ( joinType == JoinType.LEFT ) {
				parametersToUse = parameters.addSubParameters( Parameters.OR );
				parametersToUse.addNullRestriction( revisionPropertyPath, true );
				parametersToUse = parametersToUse.addSubParameters( Parameters.AND );
			}
			MiddleIdData referencedIdData = new MiddleIdData(
					configuration,
					enversService.getEntitiesConfigurations().get( entityName ).getIdMappingData(),
					null,
					entityName,
					true
			);
			enversService.getAuditStrategy().addEntityAtRevisionRestriction(
					configuration,
					queryBuilder,
					parametersToUse,
					revisionPropertyPath,
					configuration.getRevisionEndFieldName(),
					true,
					referencedIdData,
					revisionPropertyPath,
					originalIdPropertyName,
					alias,
					queryBuilder.generateAlias(),
					true
			);
		}
	}

	private void createComponentJoin(Configuration configuration) {
		String originalIdPropertyName = configuration.getOriginalIdPropertyName();
		String revisionPropertyPath = configuration.getRevisionNumberPath();
		if ( componentDescription.getType() == ComponentType.MANY ) {
			// join middle_entity
			Parameters joinConditionParameters = queryBuilder.addJoin(
					joinType,
					componentDescription.getAuditMiddleEntityName(),
					alias,
					false
			);

			String middleOriginalIdPropertyPath = alias + "." + originalIdPropertyName;
			// join condition: owner.reference_id = middle.id_ref_ing
			String ownerPrefix = ownerAlias + "." + originalIdPropertyName;
			MiddleIdData middleIdData = componentDescription.getMiddleIdData();
			middleIdData.getPrefixedMapper().addIdsEqualToQuery(
					joinConditionParameters,
					middleOriginalIdPropertyPath,
					middleIdData.getOriginalMapper(),
					ownerPrefix
			);

			// filter revisions of middle entity
			Parameters middleParameters = queryBuilder.addParameters( alias );
			Parameters middleParametersToUse = middleParameters;
			if ( joinType == JoinType.LEFT ) {
				middleParametersToUse = middleParameters.addSubParameters( Parameters.OR );
				middleParametersToUse.addNullRestriction( revisionPropertyPath, true );
				middleParametersToUse = middleParametersToUse.addSubParameters( Parameters.AND );
			}
			configuration.getAuditStrategy().addAssociationAtRevisionRestriction(
					queryBuilder,
					middleParametersToUse,
					revisionPropertyPath,
					configuration.getRevisionEndFieldName(),
					true,
					middleIdData,
					componentDescription.getAuditMiddleEntityName(),
					middleOriginalIdPropertyPath,
					revisionPropertyPath,
					originalIdPropertyName,
					alias,
					true
			);

			// filter deleted middle entities
			String middleRevTypePropertyPath = middleOriginalIdPropertyPath + "." + configuration.getRevisionTypePropertyName();
			if ( joinType == JoinType.LEFT ) {
				middleParametersToUse = middleParameters.addSubParameters( Parameters.OR );
				middleParametersToUse.addNullRestriction( middleRevTypePropertyPath, false );
			}
			middleParametersToUse.addWhereWithParam( middleRevTypePropertyPath, false, "!=", RevisionType.DEL );
		}
		else {
			// ComponentType.ONE
			/*
			 * The properties of a single component are directly mapped on the owner entity. Therefore no join would be
			 * required to access those properties (except the case an explicit on-clause has been specified). However,
			 * the user has supplied an alias and may be accessing properties of this component through that alias: If
			 * no join is generated, the 'virtual' alias has to be retranslated to the owning entity alias. To keep
			 * things simple a join on the owning entity itself is generated. The join is cheaper than other audit joins
			 * because we can join on the complete primary key (id + rev) and do not have to range filter on the target
			 * revision number.
			 */
			String targetEntityName = configuration.getAuditEntityName( entityName );
			Parameters joinConditionParameters = queryBuilder.addJoin( joinType, targetEntityName, alias, false );

			// join condition: owner.reference_id = middle.id_reference_id
			String ownerPrefix = ownerAlias + "." + originalIdPropertyName;
			String middleOriginalIdPropertyPath = alias + "." + originalIdPropertyName;
			IdMapper idMapper = enversService.getEntitiesConfigurations().get( entityName ).getIdMapper();
			idMapper.addIdsEqualToQuery( joinConditionParameters, ownerPrefix, middleOriginalIdPropertyPath );

			// join condition: owner.rev=middle.rev
			joinConditionParameters.addWhere( ownerAlias, revisionPropertyPath, "=", alias, revisionPropertyPath );
		}
	}

	@Override
	public void registerProjection(final String entityName, AuditProjection projection) {
		parent.registerProjection( entityName, projection );
	}

}
