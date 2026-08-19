import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class PageHeaderService {

  readonly detailTitle = signal<string | null>(null);

  show(title: string): void {
    this.detailTitle.set(title);
  }

  clear(): void {
    this.detailTitle.set(null);
  }
}
