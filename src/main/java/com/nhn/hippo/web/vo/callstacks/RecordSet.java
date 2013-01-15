package com.nhn.hippo.web.vo.callstacks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.nhn.hippo.web.calltree.span.SpanAlign;
import com.profiler.common.AnnotationNames;
import com.profiler.common.bo.AnnotationBo;
import com.profiler.common.bo.SpanBo;
import com.profiler.common.bo.SubSpanBo;
import com.profiler.common.util.AnnotationUtils;

/**
 * @author netspider
 */
public class RecordSet {
    private long startTime = -1;
    private long endTime = -1;

    private final List<Record> recordset;
    private String applicationName;
    
    public RecordSet(List<SpanAlign> spanAligns) {
        recordset = new ArrayList<Record>();
        addSpanRecord(spanAligns);
    }

    public Iterator<Record> getIterator() {
        return recordset.iterator();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isStartTimeSet() {
        return startTime != -1;
    }

    public boolean isEndTimeSet() {
        return endTime != -1;
    }
    
    public String getApplicationName() {
		return applicationName;
	}

	private void addAnnotationRecord(int depth, List<AnnotationBo> annotationBoList) {
        for (AnnotationBo ann : annotationBoList) {
            String annKey = ann.getKey();
            if (AnnotationNames.API.equals(annKey) || AnnotationNames.API_ID.equals(annKey))
                continue;

            if (AnnotationNames.EXCEPTION.equals(annKey)) {
                recordset.add(new Record(depth, false, ann.getValue().toString(), null, 0L, 0L, null, null));
            } else if (AnnotationNames.SQL_BINDVALUE.equals(annKey)) {
                recordset.add(new Record(depth, false, ann.getKey(), ann.getValue().toString(), 0L, 0L, null, null));
            } else if (AnnotationNames.SQL.equals(annKey)) {
                recordset.add(new Record(depth, false, ann.getKey(), ann.getValue().toString(), 0L, 0L, null, null));
            }
        }
    }

    private void addSpanRecord(List<SpanAlign> spanAligns) {
        boolean marked = false;

        for (SpanAlign sa : spanAligns) {
            if (sa.isRoot()) {
                SpanBo span = sa.getSpan();
                AnnotationUtils.sortAnnotationListByKey(span);
                String method = (String) AnnotationUtils.getDisplayMethod(span);
                String arguments = (String) AnnotationUtils.getDisplayArgument(span);

                long begin = span.getStartTime();
                long elapsed = span.getElapsed();

                if (!marked) {
                    setStartTime(begin);
                    setEndTime(begin + elapsed);
                    applicationName = arguments;
                    marked = true;
                }

                recordset.add(new Record(sa.getDepth(), true, method, arguments, begin, elapsed, span.getAgentId(), span.getServiceName()));
                addAnnotationRecord(sa.getDepth() + 1, span.getAnnotationBoList());
            } else {
                SubSpanBo subSpan = sa.getSubSpanBo();

                AnnotationUtils.sortAnnotationListByKey(subSpan);
                String method = (String) AnnotationUtils.getDisplayMethod(subSpan);
                Object arguments = AnnotationUtils.getDisplayArgument(subSpan);

                long begin = sa.getSpan().getStartTime() + subSpan.getStartElapsed();
                long elapsed = subSpan.getEndElapsed();

                if (!marked) {
                    setStartTime(begin);
                    setEndTime(begin + elapsed);
                    marked = true;
                }

                recordset.add(new Record(sa.getDepth(), true, method, (arguments != null) ? arguments.toString() : "", begin, elapsed, subSpan.getAgentId(), subSpan.getServiceName()));
                addAnnotationRecord(sa.getDepth() + 1, subSpan.getAnnotationBoList());
            }
        }
    }
}
