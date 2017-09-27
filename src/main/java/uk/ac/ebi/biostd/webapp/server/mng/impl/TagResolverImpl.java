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

package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import uk.ac.ebi.biostd.authz.AccessTag;
import uk.ac.ebi.biostd.authz.Tag;
import uk.ac.ebi.biostd.db.TagResolver;

public class TagResolverImpl implements TagResolver {

    private EntityManager em;

    public TagResolverImpl(EntityManager emngr) {
        em = emngr;
    }

    @Override
    public Tag getTagByName(String clsfName, String tagName) {
        Query q = em.createNamedQuery("Tag.getByName");

        q.setParameter("tname", tagName);
        q.setParameter("cname", clsfName);

        @SuppressWarnings("unchecked")
        List<Tag> res = q.getResultList();

        if (res.size() == 0) {
            return null;
        }

        return res.get(0);
    }

    @Override
    public AccessTag getAccessTagByName(String tagName) {
        Query q = em.createNamedQuery("AccessTag.getByName");

        q.setParameter("name", tagName);

        @SuppressWarnings("unchecked")
        List<AccessTag> res = q.getResultList();

        if (res.size() == 0) {
            return null;
        }

        return res.get(0);
    }

}
