import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class OnlineCoursesAnalyzer {

    List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
                        Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
                        Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                        Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
                        Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                        Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
                        Double.parseDouble(info[21]), Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean isContain(String s1, String s2){
        for (String str:s1.replaceAll("\"","").split(", ")){
            if (str.contains(s2)&&str.length()==s2.length()) return true;
        }
        return false;
    }
    //1
    public Map<String, Integer> getPtcpCountByInst() {
        return courses.stream().collect(Collectors.groupingBy(c-> c.institution,Collectors.summingInt(
                c->c.participants)));
    }

    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Map<String,Integer> map=courses.stream().collect(Collectors.groupingBy(c-> c.institution+
                "-"+c.subject,Collectors.summingInt(c->c.participants)));
        //descending order of count
        Map<String,Integer> result=new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .forEachOrdered(e->result.put(e.getKey().replaceAll("\"",""),
                        e.getValue()));
        return result;
    }

    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        List<String> nameList=new ArrayList<>();
        for (Course c:courses){
            if (c.instructors.contains(",")){
                for (String v:c.instructors.replaceAll("\"","").split(", ")){
                    if (!nameList.contains(v)) nameList.add(v);
                }
            }
            else if (!nameList.contains(c.instructors)) nameList.add(c.instructors);
        }   //get nameList
        Map<String, List<List<String>>> result=new HashMap<>();
        for (String n:nameList){
            //Get list0
            List<String> list0= new ArrayList<>();
            courses.stream().filter(c->c.instructors.equals(n))
                    .forEach(c->list0.add(c.title.replaceAll("\"","")));
            //Get list1
            List<String> list1=new ArrayList<>();
            for (Course c:courses){
                if (c.instructors.contains(",")&&isContain(c.instructors,n)) {
                    list1.add(c.title.replaceAll("\"",""));
                }
            }
            List<List<String>> courseList=Arrays.asList(list0.stream().distinct().sorted().toList()
                    ,list1.stream().distinct().sorted().toList());
            //get result
            result.put(n,courseList);
        }
        return result;
    }

    //4
    public List<String> getCourses(int topK, String by) {
        List<String> result=new ArrayList<>();
        //By hours
        if (Objects.equals(by, "hours")){
            courses.sort(Comparator.comparingDouble(c-> c.totalHours));
            Collections.reverse(courses);   //descending order sort
            for (Course c:courses){
                if (!result.contains(c.title.replaceAll("\"",""))){
                    result.add(c.title.replaceAll("\"",""));
                }
                if (result.size()==topK) break;
            }
            //add identical course to result
            return result;
        }
        //By participants
        else if (Objects.equals(by, "participants")){
            courses.sort(Comparator.comparingInt(c-> c.participants));
            Collections.reverse(courses);
            for (Course c:courses){
                if (!result.contains(c.title)){
                    result.add(c.title);
                }
                if (result.size()==topK) break;
            }
            return result;
        }
        return null;
    }

    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        List<String> result=new ArrayList<>();
        //filter list by the given criteria
        List<Course> copy= courses.stream()
                .filter(c->c.subject.toLowerCase().contains(courseSubject.toLowerCase()))
                .filter(c->c.percentAudited>=percentAudited)
                .filter(c->c.totalHours<=totalCourseHours)
                .toList();
        //add identical course to result
        for (Course c:copy) {
            if (!result.contains(c.title.replaceAll("\"",""))) {
                result.add(c.title.replaceAll("\"",""));
            }
        }
        Collections.sort(result);   //alphabetical order
        return result;
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        List<String> result=new ArrayList<>();
        //Map of courseNumber to age similar
        Map<String,Double> map1=courses.stream()
                .collect(Collectors.groupingBy(c->c.number,Collectors
                        .collectingAndThen(Collectors.averagingDouble(c->(c.medianAge)),
                                v->(age-v)*(age-v))));
        //Map of courseNumber to gender similar
        Map<String,Double> map2=courses.stream()
                .collect(Collectors.groupingBy(c->c.number,Collectors
                        .collectingAndThen(Collectors.averagingDouble(c->c.percentMale),
                                v->(gender*100-v)*(gender*100-v))));
        //Map of courseNumber to bachelor similar
        Map<String,Double> map3=courses.stream()
                .collect(Collectors.groupingBy(c->c.number,Collectors
                        .collectingAndThen(Collectors.averagingDouble(c->c.percentDegree),
                                v->(isBachelorOrHigher*100-v)*(isBachelorOrHigher*100-v))));
        //Map of courseNumber to similar value
        map1.forEach((k,v)->map2.merge(k,v,Double::sum));
        map2.forEach((k,v)->map3.merge(k,v,Double::sum));
        //Get a map of course number to the latest course
        courses.sort(new Comparator<Course>() {
            @Override
            public int compare(Course o1, Course o2) {
                return o2.launchDate.compareTo(o1.launchDate);
            }
        });
        Map<String,String> course=new HashMap<>();
        for (Course c: courses){
            if (!course.containsKey(c.number)){
                course.put(c.number,c.title.replaceAll("\"",""));
            }
        }
        //Get result
        Map<String,Double> map4=new HashMap<>();
        for (String k:course.keySet()) map4.put(course.get(k),map3.get(k));
        Map<String,Double> valMap=new LinkedHashMap<>();
        map4.entrySet().stream()
                .sorted(Map.Entry.<String,Double>comparingByValue()
                        .thenComparing(Map.Entry::getKey)
                )
                .forEachOrdered(e->valMap.put(e.getKey(),e.getValue()));

        for (String k:valMap.keySet()){
            if (!result.contains(k)){
                result.add(k);
            }
            if (result.size()==10) break;
        }
        return result;
    }
}
class Course {
    String institution;
    String number;
    Date launchDate;
    String title;
    String instructors;
    String subject;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;

    public Course(String institution, String number, Date launchDate,
                  String title, String instructors, String subject,
                  int year, int honorCode, int participants,
                  int audited, int certified, double percentAudited,
                  double percentCertified, double percentCertified50,
                  double percentVideo, double percentForum, double gradeHigherZero,
                  double totalHours, double medianHoursCertification,
                  double medianAge, double percentMale, double percentFemale,
                  double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) title = title.substring(1);
        if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
        this.title = title;
        if (instructors.startsWith("\"")) instructors = instructors.substring(1);
        if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
        this.instructors = instructors;
        if (subject.startsWith("\"")) subject = subject.substring(1);
        if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
        this.subject = subject;
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }
}