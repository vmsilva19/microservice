/*
 * Copyright 2020, Ray Elenteny
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.solutechconsulting.media.service.jpa;

import com.solutechconsulting.media.model.Audio;
import com.solutechconsulting.media.model.Movie;
import com.solutechconsulting.media.model.TelevisionShow;
import com.solutechconsulting.media.service.AbstractMediaService;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Named;
import javax.interceptor.Interceptor;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.stream.Stream;

@ApplicationScoped
@ActivateRequestContext
@Named(JpaMediaService.SERVICE_NAME)
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class JpaMediaService extends AbstractMediaService {

  public static final String SERVICE_NAME = "JpaMediaService";

  private Logger logger = LoggerFactory.getLogger(JpaMediaService.class.getName());

  @Inject
  EntityManager entityManager;

  @Override
  @Transactional
  protected Flowable<Movie> doGetMovies() {
    Stream<MovieEntity> movieEntityStream = entityManager.createQuery("SELECT m FROM MovieEntity m",
        MovieEntity.class).getResultStream();
    return movieStreamToFlowable(movieEntityStream);
  }

  @Override
  @Transactional
  protected Flowable<Movie> doSearchMovies(String movieText) {
    String lcMovieText = "'%" + movieText.toLowerCase() + "%'";
    String queryString =
        "SELECT m FROM MovieEntity m WHERE lower(m.title) LIKE " + lcMovieText + " OR lower(m" +
            ".tagline) LIKE " + lcMovieText + " OR lower(m.summary) LIKE " + lcMovieText;

    logger.debug(queryString);

    Stream<MovieEntity> movieEntityStream = entityManager.createQuery(queryString,
        MovieEntity.class).getResultStream();
    return movieStreamToFlowable(movieEntityStream);
  }

  protected Flowable<Movie> movieStreamToFlowable(Stream<MovieEntity> movieEntityStream) {
    Observable<MovieEntity> observable = Observable.create(emitter -> {
      try {
        movieEntityStream.forEach(emitter::onNext);
        movieEntityStream.close();
        emitter.onComplete();
      } catch (Exception e) {
        logger.error("Error building movie stream.", e);
        emitter.onError(e);
      }
    });

    return observable.toFlowable(BackpressureStrategy.BUFFER).map(MovieEntity::getMovie);
  }

  @Override
  @Transactional
  protected Flowable<Audio> doGetAudio() {
    Stream<AudioEntity> audioEntityStream = entityManager.createQuery("SELECT a FROM AudioEntity a",
        AudioEntity.class).getResultStream();
    return audioStreamToFlowable(audioEntityStream);
  }

  @Override
  @Transactional
  protected Flowable<Audio> doSearchAudio(String audioText) {
    String lcAudioText = "'%" + audioText.toLowerCase() + "%'";
    String queryString =
        "SELECT a FROM AudioEntity a WHERE lower(a.title) LIKE " + lcAudioText
            + " OR lower(a.album) LIKE "
            + lcAudioText + " OR lower(a.albumArtist) LIKE " + lcAudioText
            + " OR lower(a.artist) LIKE "
            + lcAudioText;

    logger.debug(queryString);

    Stream<AudioEntity> audioEntityStream = entityManager.createQuery(queryString,
        AudioEntity.class).getResultStream();
    return audioStreamToFlowable(audioEntityStream);
  }

  @Override
  @Transactional
  protected Flowable<Audio> doGetAudioTracks(String albumTitle) {
    String lcAlbumTitle = albumTitle.toLowerCase();
    String queryString =
        "SELECT a FROM AudioEntity a WHERE lower(a.album) = '" + lcAlbumTitle + "'";

    logger.debug(queryString);

    Stream<AudioEntity> audioEntityStream = entityManager.createQuery(queryString,
        AudioEntity.class).getResultStream();
    return audioStreamToFlowable(audioEntityStream);
  }

  protected Flowable<Audio> audioStreamToFlowable(Stream<AudioEntity> audioEntityStream) {
    Observable<AudioEntity> observable = Observable.create(emitter -> {
      try {
        audioEntityStream.forEach(emitter::onNext);
        audioEntityStream.close();
        emitter.onComplete();
      } catch (Exception e) {
        logger.error("Error building audio stream.", e);
        emitter.onError(e);
      }
    });

    return observable.toFlowable(BackpressureStrategy.BUFFER).map(AudioEntity::getAudio);
  }

  @Override
  @Transactional
  protected Flowable<TelevisionShow> doGetTelevisionShows() {
    Stream<TelevisionShowEntity> showEntityStream = entityManager.createQuery(
        "SELECT s FROM TelevisionShowEntity s", TelevisionShowEntity.class).getResultStream();

    return showStreamToFlowable(showEntityStream);
  }

  @Override
  @Transactional
  protected Flowable<TelevisionShow> doSearchTelevisionShows(String showText) {
    String lcShowText = "'%" + showText.toLowerCase() + "%'";
    String queryString =
        "SELECT s FROM TelevisionShowEntity s WHERE lower(s.title) LIKE " + lcShowText + "" +
            " OR lower(s.seriesTitle) LIKE " + lcShowText + " OR lower(s.summary) LIKE "
            + lcShowText;

    logger.debug(queryString);

    Stream<TelevisionShowEntity> showEntityStream = entityManager.createQuery(queryString,
        TelevisionShowEntity.class).getResultStream();

    return showStreamToFlowable(showEntityStream);
  }

  @Override
  @Transactional
  protected Flowable<TelevisionShow> doGetEpisodes(String seriesTitle, int season) {
    String queryString =
        "SELECT s FROM TelevisionShowEntity s WHERE lower(s.seriesTitle) = '" + seriesTitle
            .toLowerCase()
            + "' AND s.season = " + season;

    logger.debug(queryString);

    Stream<TelevisionShowEntity> showEntityStream = entityManager.createQuery(queryString,
        TelevisionShowEntity.class).getResultStream();

    return showStreamToFlowable(showEntityStream);
  }

  @Override
  @Transactional
  protected Flowable<TelevisionShow> doGetSeries(String seriesTitle) {
    String queryString =
        "SELECT s FROM TelevisionShowEntity s WHERE lower(s.seriesTitle) = '" + seriesTitle
            .toLowerCase() + "'";

    logger.debug(queryString);

    Stream<TelevisionShowEntity> showEntityStream = entityManager.createQuery(queryString,
        TelevisionShowEntity.class).getResultStream();

    return showStreamToFlowable(showEntityStream);
  }

  protected Flowable<TelevisionShow> showStreamToFlowable(
      Stream<TelevisionShowEntity> showEntityStream) {
    Observable<TelevisionShowEntity> observable = Observable.create(emitter -> {
      try {
        showEntityStream.forEach(emitter::onNext);
        showEntityStream.close();
        emitter.onComplete();
      } catch (Exception e) {
        logger.error("Error building television show stream.", e);
        emitter.onError(e);
      }
    });

    return observable.toFlowable(BackpressureStrategy.BUFFER)
        .map(TelevisionShowEntity::getTelevisionShow);
  }

  protected EntityManager getEntityManager() {
    return entityManager;
  }

  @Override
  protected String getMetricsPrefix() {
    return JpaMediaService.class.getName();
  }
}
