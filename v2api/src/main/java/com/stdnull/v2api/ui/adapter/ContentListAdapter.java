package com.stdnull.v2api.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.skyinu.annotations.BindView;
import com.skyinu.gradlebutterknife.GradleButterKnife;
import com.stdnull.baselib.utils.TimeUtils;
import com.stdnull.v2api.R;
import com.stdnull.v2api.R2;
import com.stdnull.v2api.model.V2ExBean;

import java.util.List;

import javax.inject.Inject;

/**
 * Created by chen on 2017/8/20.
 */

public class ContentListAdapter extends RecyclerView.Adapter<ContentListAdapter.ItemViewHolder> {
    private List<V2ExBean> mContents;
    private LayoutInflater mInflater;
    private Context mContext;

    @Inject
    public ContentListAdapter(Context context){
        this.mContext = context;
        mInflater = LayoutInflater.from(context);
    }
    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View root = mInflater.inflate(R.layout.v2_content_list_item, parent, false);
        ItemViewHolder holder = new ItemViewHolder(root);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
        if(mContents == null || mContents.isEmpty()){
            return;
        }
        V2ExBean v2ExBean = mContents.get(position);
        holder.mMessageBrief.setText(v2ExBean.getTitle());
        holder.mTopic.setText(v2ExBean.getNode().getTitle());
        holder.mPromulgator.setText(v2ExBean.getMember().getUsername());
        holder.mLatestReplyTime.setText(TimeUtils.calculateTimeGap(v2ExBean.getLast_touched()));
        holder.mReplyCount.setText(Integer.toString(v2ExBean.getReplies()));
        holder.mMessgeImage.setImageURI(Uri.parse("http:" + v2ExBean.getMember().getAvatar_normal()));
        holder.mPostItemContent.setVisibility(View.GONE);
        holder.mPostItemContent.setText(v2ExBean.getContent());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //don't care the real state
                holder.mPostItemContent.setVisibility(View.VISIBLE);
                holder.mPostItemContent.startAnimation(
                        AnimationUtils.loadAnimation(mContext, R.anim.v2_content_show_anim));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mContents == null? 0: mContents.size();
    }

    public void setContents(List<V2ExBean> contents) {
        this.mContents = contents;
    }

    class ItemViewHolder extends RecyclerView.ViewHolder{
        @BindView(R2.id.v2_item_image_view)
        SimpleDraweeView mMessgeImage;
        @BindView(R2.id.v2_item_text_brief)
        TextView mMessageBrief;
        @BindView(R2.id.v2_topic_view)
        TextView mTopic;
        @BindView(R2.id.v2_promulgator_view)
        TextView mPromulgator;
        @BindView(R2.id.v2_latest_reply_time_view)
        TextView mLatestReplyTime;
        @BindView(R2.id.v2_reply_count)
        TextView mReplyCount;
        @BindView(R2.id.post_item_content)
        TextView mPostItemContent;

        public ItemViewHolder(View itemView) {
            super(itemView);
            GradleButterKnife.bind(this, itemView);
        }
    }
}
