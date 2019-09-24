package com.example.popularmovies2.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.popularmovies2.model.Movie;


@Dao
public interface MovieDao {
    @Query("SELECT * FROM movie")
    LiveData<Movie[]> loadAllMovies();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertMovie(Movie movie);

    @Query("DELETE FROM movie WHERE movieId = :movieId")
    void deleteMovie(int movieId);

    @Query("SELECT * FROM movie WHERE movieId = :movieId")
    LiveData<Movie> loadMovieById(int movieId);
}
