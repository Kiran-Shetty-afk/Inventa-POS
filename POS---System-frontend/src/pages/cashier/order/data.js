  export const getStatusBadgeVariant = (status) => {
    switch (status) {
      case 'completed':
        return 'success';
      case 'pending':
        return 'warning';
      case 'cancelled':
        return 'destructive';
      default:
        return 'secondary';
    }
  };

      // Get status color
       export const getStatusColor = (status) => {
          switch (status) {
            case 'completed':
              return '#10b981';
            case 'pending':
              return '#f59e0b';
            case 'cancelled':
              return '#ef4444';
            default:
              return '#6b7280';
          }
        };

   export const parseOrderDate = (date) => {
    if (!date) return null;

    if (date instanceof Date) {
      return Number.isNaN(date.getTime()) ? null : date;
    }

    // Order timestamps are LocalDateTime values emitted by the Docker service,
    // whose clock runs in UTC. Treat timezone-free timestamps as UTC instants.
    const value = typeof date === 'string'
      ? /^\d{4}-\d{2}-\d{2}$/.test(date)
        ? `${date}T00:00:00`
        : !/(Z|[+-]\d{2}:?\d{2})$/i.test(date)
          ? `${date}Z`
          : date
      : date;
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  };

   export const formatDate = (date) => {
    const parsed = parseOrderDate(date);
    if (!parsed) return "-";
    return parsed.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

    export const getPaymentModeLabel = (mode) => {
      switch (mode) {
        case 'CASH':
          return 'Cash';
        case 'CARD':
          return 'Card';
        case 'UPI':
          return 'UPI';
        default:
          return mode;
      }
    };
