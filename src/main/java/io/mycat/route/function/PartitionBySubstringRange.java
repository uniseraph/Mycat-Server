package io.mycat.route.function;

import io.mycat.config.model.rule.RuleAlgorithm;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 该算法截取部分字符串，转成十进制数值进行范围查找，要求范围配置是连续的。
 */
public class PartitionBySubstringRange extends AbstractPartitionAlgorithm implements RuleAlgorithm {

    /**
     * 从fromIndex下标开始截取字符串。<br>
     * 如果fromIndex为负数，则表示从右向左截取，截取起始下标为：n+fromIndex，n为字符串长度。
     */
    private int fromIndex;

    /**
     * 截取字符串长度
     */
    private int length;

    /**
     * 截取后的字符串是多少进制，算法根据该参数计算十进制值用于后续范围查找
     */
    private int radix = 10;

    /**
     * 指定十进制值范围与对应的节点。<br>
     * 格式（#开头表示注释行）：<br>
     * 0-255=0<br>
     * 256-511=1<br>
     * 512-767=2<br>
     * 768-1023=3<br>
     */
    private String mapFile;

    public void setFromIndex(int fromIndex) {
        this.fromIndex = fromIndex;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setRadix(int radix) {
        this.radix = radix;
    }

    public void setMapFile(String mapFile) {
        this.mapFile = mapFile;
    }

    private int[] endValues;
    private int[] nodeIndexes;

    @Override
    public void init() {
        InputStream input = PartitionBySubstringRange.class.getResourceAsStream(mapFile);
        if (input == null) {
            throw new RuntimeException("Cannot find the map file in classpath: " + mapFile);
        }

        try {
            List<String> lines = IOUtils.readLines(input, "UTF-8");
            List<Range> ranges = new ArrayList<Range>();
            int lineNo = 0;
            for (String line : lines) {
                if (line == null) {
                    continue;
                }

                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }

                if (line.startsWith("#")) {
                    continue;
                }

                lineNo++;

                String[] pair = line.split("\\s*=\\s*");
                if (pair.length != 2) {
                    throw new RuntimeException("Illegal map format in line " + lineNo + ": " + line + " in map file: " + mapFile);
                }

                int nodeIndex;
                try {
                    nodeIndex = Integer.parseInt(pair[1]);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Illegal node index in line " + lineNo + ": " + line + " in map file: " + mapFile);
                }

                String[] values = pair[0].split("\\s*\\-\\s*");
                if (values.length != 2) {
                    throw new RuntimeException("Illegal value range format in line " + lineNo + ": " + line + " in map file: " + mapFile);
                }

                int startValue;
                try {
                    startValue = Integer.parseInt(values[0]);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Illegal start value in line " + lineNo + ": " + line + " in map file: " + mapFile);
                }

                int endValue;
                try {
                    endValue = Integer.parseInt(values[1]);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Illegal end value in line " + lineNo + ": " + line + " in map file: " + mapFile);
                }

                if (startValue > endValue) {
                    throw new RuntimeException("Illegal value range in line " + lineNo + ": " + line + " in map file: " + mapFile);
                }

                ranges.add(new Range(startValue, endValue, nodeIndex));
            }

            int n = ranges.size();
            if (n == 0) {
                throw new RuntimeException("Empty map file: " + mapFile);
            }

            Collections.sort(ranges);

            // 检查区间连续性
            int preValue = -1;
            endValues = new int[n];
            nodeIndexes = new int[n];
            int i = 0;
            for (Range r : ranges) {
                if (r.startValue != preValue + 1) {
                    throw new RuntimeException("Missing value between " + preValue + " and " + r.startValue + " in map file: " + mapFile);
                }

                preValue = r.endValue;
                endValues[i] = r.endValue;
                nodeIndexes[i] = r.nodeIndex;
                i++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to init algorithm: PartitionBySubstringRange", e);
        } finally {
            try {
                input.close();
            } catch (IOException e) {

            }
        }
    }


    public Integer calculate(String columnValue) {
        if (columnValue == null) {
            throw new RuntimeException("Null column value for PartitionBySubstringRange: " + columnValue);
        }

        int total = columnValue.length();
        if (total < length) {
            throw new RuntimeException("Too short column value for PartitionBySubstringRange: "
                    + columnValue + " (>=" + length + " characters required)");
        }

        int from = fromIndex < 0 ? fromIndex - length + 1 : fromIndex;
        int end = from + length;
        if (fromIndex < 0 && end == 0) {
            end = total;
        }

        String substr = StringUtils.substring(columnValue, from, end);
        int value = Integer.parseInt(substr, radix);
        int index = Arrays.binarySearch(endValues, value);
        if (index == endValues.length) {
            throw new RuntimeException("Too large column value for PartitionBySubstringRange: "
                    + columnValue + ", value=" + value + " (max=" + endValues[endValues.length - 1] + ")");
        }

        if (index < 0) {
            index = - index - 1;
        }

        return nodeIndexes[index];
    }


    public int getPartitionNum() {
        return nodeIndexes.length;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private static class Range implements Comparable<Range> {
        private final int startValue;
        private final int endValue;
        private final int nodeIndex;

        public Range(int startValue, int endValue, int nodeIndex) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.nodeIndex = nodeIndex;
        }

        @Override
        public int compareTo(Range o) {
            return startValue - o.startValue;
        }
    }

}
