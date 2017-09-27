/**
 * Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * @author Mikhail Gostev <gostev@gmail.com>
 **/

package uk.ac.ebi.biostd.webapp.server.mng;

import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexManager implements Runnable {

    private static final long PROGRESS_LOG_PERIOD = 5 * 60 * 1000L; // 5min
    private static Logger log;
    private EntityManagerFactory entityManagerFactory;

    public IndexManager(EntityManagerFactory emf) {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }

        entityManagerFactory = emf;
    }

    public static void rebuildIndex(EntityManagerFactory entityManagerFactory) {
        IndexManager im = new IndexManager(entityManagerFactory);

        new Thread(im).start();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Index manager thread");

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

        MassIndexer idxr = fullTextEntityManager.createIndexer();

        idxr.threadsToLoadObjects(6);
        idxr.progressMonitor(new IndexingMonitor());

        try {
            log.info("Starting Hibernate indexer");

            idxr.startAndWait();

        } catch (InterruptedException e) {
            log.error("Can't initialize Hibernate search: " + e.getMessage());
        }

        if (fullTextEntityManager.isOpen()) {
            fullTextEntityManager.close();
        }

        if (entityManager.isOpen()) {
            entityManager.close();
        }
    }

    static class IndexingMonitor implements MassIndexerProgressMonitor {

        private final AtomicLong documentsDoneCounter = new AtomicLong();
        private final AtomicLong totalCounter = new AtomicLong();
        private volatile long lastTime = 0;

        @Override
        public void documentsAdded(long increment) {
            long current = documentsDoneCounter.addAndGet(increment);

            synchronized (this) {
                long now = System.currentTimeMillis();

                if (now - lastTime > PROGRESS_LOG_PERIOD) {
                    lastTime = now;

                    log.info("Entities done so far: " + current + "/" + totalCounter.get());
                }
            }
        }

        @Override
        public void documentsBuilt(int number) {
        }

        @Override
        public void entitiesLoaded(int size) {
        }

        @Override
        public void addToTotalCount(long count) {
            long tot = totalCounter.addAndGet(count);
            log.info("Indexer total count changed (+" + count + "): " + tot);
        }

        @Override
        public void indexingCompleted() {
            log.info("Indexer job completed: " + documentsDoneCounter.get() + " entities have been indexed");

        }

    }

}
