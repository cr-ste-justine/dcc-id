/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.id.client.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import lombok.val;
import org.icgc.dcc.common.core.util.UUID5;
import org.icgc.dcc.id.client.core.IdClient;
import org.icgc.dcc.id.core.IdentifierException;
import org.icgc.dcc.id.core.Prefixes;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static com.fasterxml.uuid.Generators.timeBasedGenerator;
import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.hash.Hashing.md5;
import static java.lang.String.format;
import static org.icgc.dcc.id.core.Prefixes.DONOR_ID_PREFIX;
import static org.icgc.dcc.id.core.Prefixes.MUTATION_ID_PREFIX;
import static org.icgc.dcc.id.core.Prefixes.SAMPLE_ID_PREFIX;
import static org.icgc.dcc.id.core.Prefixes.SPECIMEN_ID_PREFIX;
import static org.icgc.dcc.id.util.Ids.validateAnalysisId;
import static org.icgc.dcc.id.util.Ids.validateUuid;

/**
 * Stateless hash based {@link IdClient} implementation that returns a stable id based on it it's inputs.
 */
public class HashIdClient implements IdClient {

  /**
   * Id generation strategy state.
   */
  private static final HashFunction MD5 = md5();
  private static final Joiner JOINER = on(":");
  private static final int RETRY_LIMIT = 1000;

  private final boolean persistInMemory;
  private final Set<String> ids = Sets.newHashSet();
  /**
   * Required for reflection in Loader
   */
  public HashIdClient(String serviceUri, String release) {
    // Empty
    persistInMemory = false;
  }

  public HashIdClient(){
    persistInMemory = false;
  }


  public HashIdClient(boolean persistInMemory){
    this.persistInMemory = persistInMemory;
  }

  @Override
  public Optional<String> getAnalysisId(String submittedAnalysisId) {
    validateAnalysisId(submittedAnalysisId);
    if (ids.contains(submittedAnalysisId)){
      return Optional.of(submittedAnalysisId);
    }
    return Optional.empty();
  }

  @Override
  public String createAnalysisId(String submittedAnalysisId) {
    validateAnalysisId(submittedAnalysisId);
    checkState(!isNullOrEmpty(submittedAnalysisId),
        "Failed to create analysis id. submittedAnalysisId: '%s'" ,
        submittedAnalysisId);
    if (persistInMemory){
      ids.add(submittedAnalysisId);
    }
    return submittedAnalysisId;
  }

  @Override
  public String generateUniqueAnalysisId() {
    String id = generateRandomUuid();
    int retryCount = 0;
    while (retryCount < RETRY_LIMIT){
      if (!ids.contains(id)){
        validateUuid(id);
        return id;
      }
      retryCount++;
      id = generateRandomUuid();
    }
    throw new IdentifierException(format("Exceeded max retry count of %s for finding unique analysis id", RETRY_LIMIT));
  }

  private String generateRandomUuid(){
    return timeBasedGenerator().generate().toString();
  }

  @Override
  public Optional<String> getDonorId(String submittedDonorId, String submittedProjectId) {
    return Optional.of(DONOR_ID_PREFIX + generateId(
        submittedDonorId,
        submittedProjectId));
  }

  @Override
  public Optional<String> getSampleId(String submittedSampleId, String submittedProjectId) {
    return Optional.of(SAMPLE_ID_PREFIX + generateId(
        submittedSampleId,
        submittedProjectId));
  }

  @Override
  public Optional<String> getSpecimenId(String submittedSpecimenId, String submittedProjectId) {
    return Optional.of(SPECIMEN_ID_PREFIX + generateId(
        submittedSpecimenId,
        submittedProjectId));
  }

  @Override
  public Optional<String> getMutationId(String chromosome, String chromosomeStart, String chromosomeEnd,
      String mutation, String mutationType, String assemblyVersion) {
    return Optional.of(MUTATION_ID_PREFIX + generateId(
        chromosome,
        chromosomeStart,
        chromosomeEnd,
        mutation,
        mutationType,
        assemblyVersion));
  }

  @Override
  public Optional<String> getFileId(String submittedFileId) {
    return Optional.of(Prefixes.FILE_ID_PREFIX + generateId(submittedFileId));
  }

  @Override
  public Optional<String> getObjectId(String analysisId, String fileName) {
    return Optional.of(UUID5.fromUTF8(UUID5.getNamespace(), Joiner.on('/').join(analysisId, fileName)).toString());
  }

  @Override
  public String createRandomAnalysisId() {
    val id = timeBasedGenerator().generate().toString();
    validateUuid(id);
    if (persistInMemory){
      ids.add(id);
    }
    return id;
  }

  @Override
  public String createDonorId(String submittedDonorId, String submittedProjectId) {
    return getDonorId(submittedDonorId, submittedProjectId).get();
  }

  @Override
  public String createMutationId(String chromosome, String chromosomeStart, String chromosomeEnd, String mutation,
      String mutationType, String assemblyVersion) {
    return getMutationId(chromosome, chromosomeStart, chromosomeEnd, mutation, mutationType, assemblyVersion).get();
  }

  @Override
  public String createSampleId(String submittedSampleId, String submittedProjectId) {
    return getSampleId(submittedSampleId, submittedProjectId).get();
  }

  @Override
  public String createSpecimenId(String submittedSpecimenId, String submittedProjectId) {
    return getSpecimenId(submittedSpecimenId, submittedProjectId).get();
  }

  @Override
  public String createFileId(String submittedFileId) {
    return getFileId(submittedFileId).get();
  }

  @Override
  public void close() throws IOException {
    // No-op
  }

  private static String generateId(String... keys) {
    return MD5.hashUnencodedChars(JOINER.join(keys)).toString();
  }

}
