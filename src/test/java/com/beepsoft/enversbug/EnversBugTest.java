package com.beepsoft.enversbug;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates a bug when having a @ManyToMany relationship with @OrderBy with an ID property of the target entity and
 * trying to load an old revision of the entity.
 *
 * Results in:
 * java.lang.IllegalArgumentException: org.hibernate.QueryException: could not resolve property: mtid of: com.beepsoft.enversbug.Language_AUD [select new list(ee__, e__) from publication_languages_AUD ee__, com.beepsoft.enversbug.Language_AUD e__ where ee__.originalId.Publication_mtid = :Publication_mtid and e__.originalId.REV.id = (select max(e2__.originalId.REV.id) from com.beepsoft.enversbug.Language_AUD e2__ where e2__.originalId.REV.id <= :revision and e__.originalId.mtid = e2__.originalId.mtid) and ee__.originalId.REV.id = (select max(ee2__.originalId.REV.id) from publication_languages_AUD ee2__ where ee2__.originalId.REV.id <= :revision and ee__.originalId.Publication_mtid = ee2__.originalId.Publication_mtid and ee__.originalId.languages_mtid = ee2__.originalId.languages_mtid) and ee__.REVTYPE != :delrevisiontype and e__.REVTYPE != :delrevisiontype and (ee__.originalId.languages_mtid = e__.originalId.mtid or (ee__.originalId.languages_mtid is null and e__.originalId.mtid is null)) order by e__.mtid]
 *
 * If I use @OrderBy("languageCode") instead of the ID field @OrderBy("mtid") then it works all right.
 *
 * Run
 *
 * 		./mvnw clean test
 *
 * to reproduce the bug.
 */
@SpringBootTest
class EnversBugTest
{
	@Autowired
	private PlatformTransactionManager platformTransactionManager;

	@Autowired
	EntityManager entityManager;

	@Test
	@Transactional
	void reproduceBug() {

		// Create a Publication with a Language in Publication.languages
		Publication pub1 = runInSeparateTransaction(transactionStatus -> {
			Publication p = new Publication();
			p.title = "Publication title";
			p.languages = new ArrayList<>();
			Language l = new Language();
			l.languageCode = "hu";
			p.languages.add(l);
			entityManager.persist(l);

			l = new Language();
			l.languageCode = "en";
			p.languages.add(l);
			entityManager.persist(l);

			l = new Language();
			l.languageCode = "fr";
			p.languages.add(l);
			entityManager.persist(l);

			entityManager.persist(p);

			return p;
		});

		// Reload publication and edit its title to have a revision
		Publication pub2 = runInSeparateTransaction(transactionStatus -> {
			Publication p = entityManager.find(Publication.class, pub1.mtid);
			p.title = "Publication title modified";
			return p;
		});

		// Load the old version of publication and try to access languages.size()
		Publication pub3 = runInSeparateTransaction(transactionStatus -> {
			AuditQuery q = AuditReaderFactory.get(entityManager).createQuery()
					.forRevisionsOfEntity(Publication.class, false, true)
					.add(AuditEntity.property("mtid").eq(pub1.mtid));
			List<Object[]> queryResult = q.getResultList();
			Publication oldObj = (Publication)queryResult.get(0)[0];
			// Breaks here
			oldObj.languages.size();
			assertEquals(oldObj.languages.get(0).languageCode, "hu");
			assertEquals(oldObj.languages.get(1).languageCode, "en");
			assertEquals(oldObj.languages.get(2).languageCode, "fr");
			return oldObj;
		});

	}

	// Simulates a Spring transaction
	private <T extends Object> T runInSeparateTransaction(TransactionCallback<T> callback){
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
			transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			transactionTemplate.setReadOnly(false);
			T obj = transactionTemplate.execute(callback);
			return obj;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}
}
