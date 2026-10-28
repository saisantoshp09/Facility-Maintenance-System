import { Directive, OnDestroy } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { Observable, Subscription } from 'rxjs';
import { AppError, Page, PageQuery } from '../models';
import { errorMessage } from './error-utils';

/** Shared paging/sorting/loading/error state for list screens. */
@Directive()
export abstract class PagedList<T> implements OnDestroy {
  items: T[] = [];
  total = 0;
  page = 0;
  size = 20;
  sort: { field: string; dir: 'asc' | 'desc' } | null = null;
  loading = false;
  error: string | null = null;
  readonly pageSizes = [10, 20, 50, 100];
  private sub?: Subscription;

  protected abstract fetch(q: PageQuery): Observable<Page<T>>;

  reload(): void {
    this.sub?.unsubscribe();
    this.loading = true;
    this.error = null;
    this.sub = this.fetch({ page: this.page, size: this.size, sort: this.sort }).subscribe({
      next: p => { this.items = p.content; this.total = p.totalElements; this.loading = false; },
      error: (e: AppError) => { this.items = []; this.total = 0; this.loading = false; this.error = errorMessage(e); }
    });
  }

  onPage(e: PageEvent): void { this.page = e.pageIndex; this.size = e.pageSize; this.reload(); }

  onSort(s: Sort): void {
    this.sort = s.direction ? { field: s.active, dir: s.direction } : null;
    this.page = 0;
    this.reload();
  }

  resetAndReload(): void { this.page = 0; this.reload(); }

  ngOnDestroy(): void { this.sub?.unsubscribe(); }
}
