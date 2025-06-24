package com.dtech.auth.util;

import lombok.extern.log4j.Log4j2;
import org.hibernate.query.Query;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.text.DecimalFormat;

@Log4j2
@Component
public class FacilityIdGenUtil implements org.hibernate.id.IdentifierGenerator {

    @Override
    public Serializable generate(org.hibernate.engine.spi.SharedSessionContractImplementor session, Object object) {
        try {
            log.info("Generating request id for " + object);

            String hql = "SELECT max(id) from ApplicationUser";

            Query<Integer> query = session.createQuery(hql, Integer.class);

            Integer nextPrimId = query.uniqueResult();
            int nextId = (nextPrimId == null ? 1 : nextPrimId);

            DecimalFormat formatter = new DecimalFormat("0000");
            log.info("Generating request successfully  " + nextId);
            return formatter.format(nextId);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
