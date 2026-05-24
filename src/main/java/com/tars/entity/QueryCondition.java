package com.tars.entity;

import lombok.Data;

/**
 * Encapsulates query parameters for filtered and paginated data retrieval.
 * <p>
 * This class provides a standardized way to pass search, filter, sort, and pagination
 * parameters from the frontend to backend services. It is used across all user roles
 * (Admin, TA, MO) for consistent query handling.
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * // Frontend sends: ?filter=status&order=desc&key=posStatus&search=open&page=2
 * QueryCondition condition = new QueryCondition();
 * condition.setFilter("status");
 * condition.setOrder("desc");
 * condition.setKey("posStatus");
 * condition.setSearch("open");
 * condition.setPage(2);
 * 
 * // Backend uses condition to build dynamic SQL/query
 * List<Position> positions = positionService.query(condition);
 * }</pre>
 * </p>
 * <p>
 * <b>Field Descriptions:</b>
 * <ul>
 *   <li><b>filter</b>: Field name to filter by (e.g., "status", "college")</li>
 *   <li><b>order</b>: Sort order - "asc" for ascending, "desc" for descending</li>
 *   <li><b>key</b>: Specific field key for filtering (e.g., "posStatus", "grade")</li>
 *   <li><b>search</b>: Search keyword or value to match</li>
 *   <li><b>page</b>: Page number for pagination (defaults to 1, must be > 0)</li>
 * </ul>
 * </p>
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/8
 * @see com.tars.service.AdminService
 * @see com.tars.service.TAService
 * @see com.tars.service.MOService
 */
@Data
public class QueryCondition {

    /**
     * Filter field name for querying specific attributes.
     * <p>
     * Examples: "status", "college", "degree", "moduleCode"
     * </p>
     */
    private String filter = "";

    /**
     * Sort order direction.
     * <ul>
     *   <li>"asc" - Ascending order</li>
     *   <li>"desc" - Descending order</li>
     *   <li>Empty string - No sorting applied</li>
     * </ul>
     */
    private String order = "";

    /**
     * Specific field key for filtering operations.
     * <p>
     * Examples: "posStatus" for position status, "grade" for student grade level
     * </p>
     */
    private String key = "";

    /**
     * Search keyword or value to match against the filter field.
     * <p>
     * Examples: "open" for open positions, "Computer Science" for major
     * </p>
     */
    private String search = "";

    /**
     * Page number for pagination (1-based index).
     * <p>
     * Defaults to 1 if null or &lt;= 0. Validated through {@link #setPage(Integer)}.
     * </p>
     *
     * @see #setPage(Integer)
     */
    private Integer page = 1;

    /**
     * Sets the page number with validation.
     * <p>
     * Ensures page number is always positive. If the provided value is null or &lt;= 0,
     * it defaults to page 1.
     * </p>
     *
     * @param page Requested page number
     */
    public void setPage(Integer page) {
        if (page != null && page > 0) {
            this.page = page;
        } else {
            this.page = 1;
        }
    }
}
