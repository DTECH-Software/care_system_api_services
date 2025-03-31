package com.dtech.claim.util;

import com.dtech.claim.dto.ClaimRequestIdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.hibernate.query.Query;

import java.io.Serializable;
import java.text.DecimalFormat;

@Log4j2
public class RequestIdGenUtil implements org.hibernate.id.IdentifierGenerator {

    private final boolean  isMedical;

    public RequestIdGenUtil(Boolean isMedical) {
        this.isMedical = isMedical;
    }


    @Override
    public Serializable generate(org.hibernate.engine.spi.SharedSessionContractImplementor session, Object object) {
        try {
            log.info("Generating request id for " + object);
            ClaimRequestIdGen claimRequestIdGen = (ClaimRequestIdGen) object;
            String staffCategory = claimRequestIdGen.getStaffCategory();
            String year = String.valueOf(claimRequestIdGen.getYear());
            String company = claimRequestIdGen.getCompany();

            String prefix = "";
            String hql = null;

            if(isMedical){
                log.info("Generating request id for medical " + object);
                prefix = "MED"+ "/" + staffCategory + "/" + year + "/" + company + "/";

                hql = "SELECT MAX(CAST(SUBSTRING(requestId, LENGTH(:prefix) + 1) AS int)) " +
                        "FROM InsuranceClaimsRequest WHERE requestId LIKE :idPrefix";
            }else{
                log.info("Generating request id for death " + object);
                prefix = "DEA"+ "/" + staffCategory + "/" + year + "/" + company + "/";

                hql = "SELECT MAX(CAST(SUBSTRING(requestId, LENGTH(:prefix) + 1) AS int)) " +
                        "FROM DeathClaimRequest WHERE requestId LIKE :idPrefix";
            }

            log.info("Generating request id set prefix {} {} ",prefix, hql);
            Query<Integer> query = session.createQuery(hql, Integer.class);
            query.setParameter("prefix", prefix);
            query.setParameter("idPrefix", prefix + "%");

            Integer maxId = query.uniqueResult();
            int nextId = (maxId == null ? 1 : maxId + 1);

            DecimalFormat formatter = new DecimalFormat("0000");
            log.info("Generating request successfully  " + nextId);
            return prefix + formatter.format(nextId);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
