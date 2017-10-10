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

public class SubmissionSearchRequest {

    private String keywords;
    private long fromCTime = Long.MIN_VALUE;
    private long toCTime = Long.MAX_VALUE;
    private long fromMTime = Long.MIN_VALUE;
    private long toMTime = Long.MAX_VALUE;
    private long fromRTime = Long.MIN_VALUE;
    private long toRTime = Long.MAX_VALUE;
    private int fromVersion = Integer.MIN_VALUE;
    private int toVersion = Integer.MAX_VALUE;
    private long ownerId;
    private String owner;
    private String accNo;
    private SortFields sortBy;
    private int skip;
    private int limit;

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public long getFromCTime() {
        return fromCTime;
    }

    public void setFromCTime(long fromCTime) {
        this.fromCTime = fromCTime;
    }

    public long getToCTime() {
        return toCTime;
    }

    public void setToCTime(long toCTime) {
        this.toCTime = toCTime;
    }

    public long getFromMTime() {
        return fromMTime;
    }

    public void setFromMTime(long fromMTime) {
        this.fromMTime = fromMTime;
    }

    public long getToMTime() {
        return toMTime;
    }

    public void setToMTime(long toMTime) {
        this.toMTime = toMTime;
    }

    public long getFromRTime() {
        return fromRTime;
    }

    public void setFromRTime(long fromRTime) {
        this.fromRTime = fromRTime;
    }

    public long getToRTime() {
        return toRTime;
    }

    public void setToRTime(long toRTime) {
        this.toRTime = toRTime;
    }

    public SortFields getSortBy() {
        return sortBy;
    }

    public void setSortBy(SortFields sortBy) {
        this.sortBy = sortBy;
    }

    public int getSkip() {
        return skip;
    }

    public void setSkip(int skip) {
        this.skip = skip;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccNo() {
        return accNo;
    }

    public void setAccNo(String accNo) {
        this.accNo = accNo;
    }

    public int getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(int fromVersion) {
        this.fromVersion = fromVersion;
    }

    public int getToVersion() {
        return toVersion;
    }

    public void setToVersion(int toVersion) {
        this.toVersion = toVersion;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public enum SortFields {
        CTime,
        MTime,
        RTime
    }


}
