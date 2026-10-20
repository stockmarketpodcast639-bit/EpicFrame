package com.example.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.R;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotAdapter extends RecyclerView.Adapter<ScreenshotAdapter.ViewHolder> {
    private final Context context;
    private List<String> screenshotUrls = new ArrayList<>();

    public ScreenshotAdapter(Context context) {
        this.context = context;
    }

    public void updateList(List<String> urls) {
        this.screenshotUrls = urls != null ? urls : new ArrayList<String>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_screenshot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = screenshotUrls.get(position);
        Glide.with(context)
                .load(url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.bg_card_movie)
                .error(R.drawable.bg_card_movie)
                .into(holder.imgScreenshot);
    }

    @Override
    public int getItemCount() {
        return screenshotUrls.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgScreenshot;

        ViewHolder(View itemView) {
            super(itemView);
            imgScreenshot = itemView.findViewById(R.id.imgScreenshot);
        }
    }
}
