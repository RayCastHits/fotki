package com.example.fotki.adapter;

import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.Rotate;
import com.bumptech.glide.request.RequestOptions;
import com.example.fotki.PhotoDetailActivity;
import com.example.fotki.R;
import com.example.fotki.Utils.FileUtils;
import java.util.List;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ImageViewHolder> {
    private final List<String> imageUrls;
    private final List<String> usernames;
    private final List<String> emails;
    private Context context;

    public ImageListAdapter(Context context, List<String> imageUrls, List<String> usernames, List<String> emails) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.usernames = usernames;
        this.emails = emails;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycleview_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        String username = usernames.get(position);
        String email = emails.get(position);

        RequestOptions requestOptions = new RequestOptions()
                .transform(new Rotate(getImageRotation(imageUrl)))
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(holder.imageView);

        holder.itemView.setOnClickListener(view -> {

            Intent intent = new Intent(context, PhotoDetailActivity.class);

            intent.putExtra("imageUrl", imageUrl);
            intent.putExtra("username", username);
            intent.putExtra("email", email);

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
    private int getImageRotation(String imageUrl) {
        try {
            Uri imageUri = Uri.parse(imageUrl);
            String imagePath = FileUtils.getPath(context, imageUri);

            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}