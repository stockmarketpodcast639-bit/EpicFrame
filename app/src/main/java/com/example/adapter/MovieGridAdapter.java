package com.example.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.R;
import com.example.model.MovieItem;
import java.util.ArrayList;
import java.util.List;

public class MovieGridAdapter extends RecyclerView.Adapter<MovieGridAdapter.ViewHolder> {
    private final Context context;
    private List<MovieItem> movies = new ArrayList<>();
    private final MovieCardAdapter.OnMovieClickListener listener;

    public MovieGridAdapter(Context context, MovieCardAdapter.OnMovieClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<MovieItem> newMovies) {
        this.movies = newMovies != null ? newMovies : new ArrayList<MovieItem>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final MovieItem movie = movies.get(position);
        holder.tvTitle.setText(movie.getCleanTitle());

        if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
            Glide.with(context)
                    .load(movie.getPosterUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.bg_card_movie)
                    .error(R.drawable.bg_card_movie)
                    .into(holder.imgPoster);
        } else {
            holder.imgPoster.setImageResource(R.drawable.bg_card_movie);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onMovieClick(movie);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView tvTitle;

        ViewHolder(View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgGridPoster);
            tvTitle = itemView.findViewById(R.id.tvGridTitle);
        }
    }
}
