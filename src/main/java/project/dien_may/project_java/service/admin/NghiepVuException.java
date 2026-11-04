package project.dien_may.project_java.service.admin;

/** Vi phạm quy tắc nghiệp vụ; message được hiển thị thẳng cho quản trị viên. */
public class NghiepVuException extends RuntimeException {

    public NghiepVuException(String message) {
        super(message);
    }
}
