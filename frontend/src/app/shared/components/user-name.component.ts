import { Component, Input } from '@angular/core';
import { UserDirectory } from '../../core/services/user-directory';

@Component({
  selector: 'app-user-name',
  template: `<span [title]="id || ''">{{ (name$ | async) }}</span>`
})
export class UserNameComponent {
  name$ = this.dir.name$(null);
  private _id: string | null | undefined;

  constructor(private dir: UserDirectory) {}

  @Input() set id(v: string | null | undefined) {
    this._id = v;
    this.name$ = this.dir.name$(v);
  }
  get id(): string | null | undefined { return this._id; }
}
