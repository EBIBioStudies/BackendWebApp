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

package uk.ac.ebi.biostd.update;

import java.util.Map;
import java.util.TreeMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;

public class UpdateTestMain {

    public static void main(String[] args) {
        Map<String, Object> conf = new TreeMap<String, Object>();

        conf.put("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        conf.put("hibernate.connection.username", "biostd");
        conf.put("hibernate.connection.password", "biostd");
        conf.put("hibernate.connection.url", "jdbc:mysql://localhost/biostd_prod?autoReconnect=true");
        conf.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        conf.put("hibernate.hbm2ddl.auto", "update");
        conf.put("hibernate.show_sql", "true");
//  conf.put("hibernate.archive.autodetection", "class, hbm");

        EntityManagerFactory fact = Persistence.createEntityManagerFactory("BioStdCoreModel", conf);

        EntityManager em = fact.createEntityManager();

        EntityTransaction trn = em.getTransaction();

        trn.begin();

        User u = em.find(User.class, 3L);

        System.out.println(u.getLogin());

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaUpdate<Submission> upd = cb.createCriteriaUpdate(Submission.class);

        Root<Submission> r = upd.from(Submission.class);

        upd.where(cb.like(r.get("accNo"), "EuropePMC"));
        upd.set(r.get("owner"), u.getId());

        em.createQuery(upd).executeUpdate();

        trn.commit();

        em.close();

        fact.close();
    }

}
