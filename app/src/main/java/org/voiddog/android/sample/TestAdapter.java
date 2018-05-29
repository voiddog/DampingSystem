package org.voiddog.android.sample;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * ┏┛ ┻━━━━━┛ ┻┓
 * ┃　　　　　　 ┃
 * ┃　　　━　　　┃
 * ┃　┳┛　  ┗┳　┃
 * ┃　　　　　　 ┃
 * ┃　　　┻　　　┃
 * ┃　　　　　　 ┃
 * ┗━┓　　　┏━━━┛
 * * ┃　　　┃   神兽保佑
 * * ┃　　　┃   代码无BUG！
 * * ┃　　　┗━━━━━━━━━┓
 * * ┃　　　　　　　    ┣┓
 * * ┃　　　　         ┏┛
 * * ┗━┓ ┓ ┏━━━┳ ┓ ┏━┛
 * * * ┃ ┫ ┫   ┃ ┫ ┫
 * * * ┗━┻━┛   ┗━┻━┛
 *
 * @author qigengxin
 * @since 2018-04-26 14:30
 */
public class TestAdapter extends RecyclerView.Adapter<TestAdapter.VH>{

    public List<String> contentList = new ArrayList<>();

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(parent.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(contentList.get(position));
    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    public static class VH extends RecyclerView.ViewHolder{

        private TextView textView;

        public VH(Context context) {
            super(new TextView(context));
            textView = (TextView) itemView;
            textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int paddingVertical = dp2px(textView.getContext(), 20);
            int paddingHorizontal = dp2px(textView.getContext(), 15);
            textView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }

        private int dp2px(Context context, float dp) {
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }

        public void bind(String content) {
            textView.setText(content);
        }
    }
}
