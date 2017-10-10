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

package uk.ac.ebi.biostd.webapp.server.export;

import java.io.PrintWriter;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.model.Submission;

public class SbmIDBagManager {

    private Logger log;

    private long[] submissionIds;

    private int offset = 0;

    private int blockSize;

    @SuppressWarnings("unchecked")
    public SbmIDBagManager(EntityManagerFactory emf, int blSz, long since) {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }

        blockSize = blSz;

        EntityManager em = emf.createEntityManager();

        Query idSelQuery = null;

        if (since > 0) {
            idSelQuery = em.createQuery("select sbm.id from " + Submission.class.getCanonicalName()
                    + " sbm WHERE sbm.mTime > :upDate AND sbm.version > 0");
            idSelQuery.setParameter("upDate", new Date(since));
        } else {
            idSelQuery = em.createQuery(
                    "select id from " + Submission.class.getCanonicalName() + " sbm WHERE sbm.version > 0");
        }

        Collection<Long> sids = idSelQuery.getResultList();

        submissionIds = new long[sids.size()];

        int i = 0;
        for (Long l : sids) {
            submissionIds[i++] = l.longValue();
        }

        Arrays.sort(submissionIds);

        log.debug("Retrieved {} submission IDs", submissionIds.length);

//  try( PrintWriter out = new PrintWriter(BackendConfig.getBaseDirectory().resolve("dump/idDump.txt").toFile()) )
//  {
//   
//   for( i=0; i < submissionIds.length; i++ )
//    out.printf("%d %d\n", i+1, submissionIds[i]);
//  }
//  catch(FileNotFoundException e)
//  {
//   e.printStackTrace();
//  }

        em.close();
    }


    public int getSubmissionCount() {
        return submissionIds.length;
    }


    public Range getSubmissionRange(PrintWriter out) {
        synchronized (submissionIds) {
            if (offset >= submissionIds.length) {
                return null;
            }

            if (log.isDebugEnabled()) {
                log.debug("Requested submissions range. Offset {} out of {} ({}%)",
                        offset, submissionIds.length, offset * 100 / submissionIds.length);
            }

            Range r = new Range(submissionIds[offset], 0);

            int oldOffs = offset;

            offset += blockSize;

            if (offset > submissionIds.length) {
                offset = submissionIds.length;
            }

            r.setMax(submissionIds[offset - 1]);

            r.setIds(submissionIds, oldOffs, offset);

            out.println("Selected range:");

            int i = 1;
            for (; oldOffs < offset; oldOffs++) {
                out.printf("R %d %d\n", i++, submissionIds[oldOffs]);
            }

            return r;
        }
    }


    public int getChunkSize() {
        return blockSize;
    }

}
